package com.example.tetris.ai;

import com.example.tetris.core.state.GameState;
import com.example.tetris.core.state.OpMove;
import com.example.tetris.core.state.PureGame;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Coordinator that queries an {@link Ai} for plans and caches the most recent result for the latest
 * {@link PureGame} snapshot.
 *
 * <p>Usage pattern: call {@link #tick(GameState)} on every frame. When the active piece locks
 * (or otherwise changes the board), {@code GameState#toCells()} should produce a different
 * {@link PureGame}; this service cancels any in-flight request for the old snapshot, starts a new
 * async decision, and returns the last completed plan (or {@code null} while thinking).</p>
 *
 * <h2>Thread-safety</h2>
 * <ul>
 *   <li>Designed to be called from the JavaFX application thread.</li>
 *   <li>Minimal cross-thread coordination: fields are {@code volatile} for visibility and short
 *       critical sections are guarded with {@code synchronized (this)} to keep state transitions
 *       atomic.</li>
 *   <li>{@link CompletableFuture#whenComplete} may run on an arbitrary thread; publication to
 *       {@link #cachedOpMove} is synchronized to avoid races.</li>
 * </ul>
 *
 * <h2>Cancellation</h2>
 *
 * <p>When a new snapshot arrives, the previous {@link #inFlight} future is cancelled. Whether that
 * is effective depends on the {@link Ai} implementation. The provided {@code FakeAi} completes
 * immediately, so cancellation is mostly for realistic AIs.</p>
 *
 * <h2>Equality of snapshots</h2>
 *
 * <p>This class relies on {@link PureGame#equals(Object)} to detect changes. Ensure your record’s
 * fields (including nested arrays) implement deep equality if required for your game semantics.</p>
 */
public class AiService {

  private final Ai ai;

  // Request state
  private volatile CompletableFuture<OpMove> inFlight;
  private volatile PureGame cachePureGame;

  // Last completed plan for the current cached snapshot
  private volatile OpMove cachedOpMove;

  /**
   * Creates a new AI coordinator.
   *
   * @param ai the AI engine used to compute plans (must not be {@code null})
   *
   * @throws NullPointerException if {@code ai} is {@code null}
   */
  public AiService(Ai ai) {
    this.ai = Objects.requireNonNull(ai, "ai is null");
  }

  /**
   * Progresses the AI request lifecycle for the given game state and returns the best-known plan
   * for the current snapshot.
   *
   * <p>Behavior:</p>
   * <ul>
   *   <li>If {@code state} is {@code null} or the game is over, returns {@code null}.</li>
   *   <li>Build a {@link PureGame} via {@link GameState#toPureGame()}.</li>
   *   <li>If the snapshot differs from the cached one, cancel the previous request (if any),
   *       clear the cached plan, and start a new async decision.</li>
   *   <li>Return the last plan produced for the current snapshot (maybe {@code null} while the
   *       AI is computing).</li>
   * </ul>
   *
   * <p>Call this every frame; it is non-blocking.</p>
   *
   * @param state the current game state
   *
   * @return an {@link OpMove} for the current snapshot, or {@code null} if none is ready
   */
  public OpMove tick(GameState state) {
    if (state == null || state.isGameOver()) {
      return null;
    }

    final PureGame snapshot = state.toPureGame();

    synchronized (this) {
      if (!snapshot.equals(cachePureGame)) {
        // New snapshot → cancel old request and clear stale plan
        cachePureGame = snapshot;
        cachedOpMove = null;

        final CompletableFuture<OpMove> prev = inFlight;
        if (prev != null) {
          // Effective only if the AI cooperates with interruption / checks isCancelled.
          prev.cancel(true);
        }

        // Start new request
        final CompletableFuture<OpMove> fut = ai.decideAsync(snapshot);
        inFlight = fut;

        fut.whenComplete((opMove, throwable) -> {
          if (throwable != null || opMove == null) {
            return; // swallow: caller will continue returning null until next plan
          }
          synchronized (this) {
            // Only publish if this future is still the current one for the snapshot.
            if (inFlight == fut) {
              cachedOpMove = opMove;
            }
          }
        });
      }
    }

    // Return the best-known plan for the current snapshot (maybe null while thinking)
    return cachedOpMove;
  }

  /**
   * Shuts down the underlying {@link Ai}. Subsequent calls to {@link #tick(GameState)} will
   * continue returning the last cached plan until a new snapshot arrives, but the AI may refuse new
   * work per its contract.
   */
  public void shutdown() {
    ai.shutdown();
  }
}
