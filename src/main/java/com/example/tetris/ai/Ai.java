package com.example.tetris.ai;

import com.example.tetris.core.state.GameIntent;
import com.example.tetris.core.state.OpMove;
import com.example.tetris.core.state.PureGame;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for Tetris AIs that compute a planned move ({@link OpMove}) for a given immutable game
 * snapshot ({@link PureGame}) and emit a {@link GameIntent} each tick to steer the active tetromino
 * toward that plan.
 *
 * <p><strong>Template:</strong> Subclasses implement {@link #submit(PureGame)} to compute
 * an {@code OpMove} asynchronously. The default {@link #getCurrentIntent(OpMove, PureGame)}
 * implements a simple "follow-the-plan" controller and provides several overridable hooks:
 * {@link #needsRotation(OpMove, PureGame)}, {@link #getCurrentX(PureGame)}, and
 * {@link #whenAligned(OpMove, PureGame)}.</p>
 *
 * <p><strong>Concurrency:</strong> This class manages a single in-flight computation.
 * When a new {@code PureGame} differs from the cached snapshot, the previous computation is
 * cancelled (best-effort) and a new one is submitted. Field access is guarded by {@code volatile}
 * visibility; no external synchronization is required.</p>
 *
 * <p><strong>Lifecycle:</strong> Call {@link #shutdown()} when the AI is no longer needed
 * so subclasses can release executors or native resources.</p>
 */
public abstract class Ai {

  /**
   * Logger for AI diagnostics and failure reporting.
   */
  protected static final Logger LOG = Logger.getLogger(Ai.class.getName());

  /**
   * Last snapshot we planned for. Used to detect when a new plan is required.
   */
  private volatile PureGame cachePureGame = null;

  /**
   * The last completed plan for {@link #cachePureGame}. May be {@code null} if not ready.
   */
  private volatile OpMove cachedOpMove = null;

  /**
   * The current async plan computation, if any.
   */
  private volatile CompletableFuture<OpMove> inFlight;

  /**
   * Advances the AI by one tick against the provided immutable {@link PureGame} snapshot.
   *
   * <p>If the snapshot differs from the last one, a new asynchronous computation is submitted
   * via {@link #submit(PureGame)}, the previous one is cancelled (best-effort), and the cached plan
   * is cleared. If the snapshot is unchanged and a plan has completed, the method emits a
   * {@link GameIntent} that moves/rotates the current piece toward that plan.</p>
   *
   * @param snapshot the current immutable game state; {@code null} returns {@code null}
   *
   * @return the next intent to apply this frame, or {@code null} if no action is needed or a plan
   *     is not yet available
   */
  public final GameIntent tick(PureGame snapshot) {
    if (snapshot == null) {
      return null;
    }
    if (shouldStartNewPlan(snapshot, cachePureGame, cachedOpMove)) {
      cachePureGame = snapshot;
      cachedOpMove = null;
      if (inFlight != null && !inFlight.isDone()) {
        inFlight.cancel(true);
      }
      inFlight = null;

      final CompletableFuture<OpMove> future = submit(snapshot);
      if (future == null) {
        onError(new NullPointerException("Ai.submit() returned null"));
        return null;
      }

      inFlight = future;
      future.whenComplete((opMove, throwable) -> {
        // Ignore completion if a newer computation has since been started.
        if (inFlight != future) {
          return;
        }
        if (throwable != null) {
          onError(throwable);
          return;
        }
        cachedOpMove = opMove;
      });
    }
    return getCurrentIntent(cachedOpMove, cachePureGame);
  }

  /**
   * Decide whether to discard any existing plan and start a new one for the given snapshot.
   *
   * <p><strong>Default:</strong> replan when
   * {@code !Objects.equals(newSnapshot, lastSnapshot)}.</p>
   *
   * <p><strong>Common overrides:</strong>
   * <ul>
   *   <li>Only replan when a new piece spawns (e.g., compare active piece IDs/types).</li>
   *   <li>Throttle replans to once every N frames or milliseconds.</li>
   *   <li>Force replan if the existing plan is incompatible with the new board (e.g., obstruction).
   *   </li>
   * </ul>
   * Keep this decision fast and side effect free; it runs every frame.</p>
   *
   * @param newSnapshot  the current snapshot (non-null)
   * @param lastSnapshot the snapshot used for {@code cachedOpMove} (maybe null)
   * @param existingPlan the last completed plan for {@code lastSnapshot} (maybe null)
   *
   * @return {@code true} to cancel any in-flight computation and submit a new one
   */
  protected boolean shouldStartNewPlan(PureGame newSnapshot, PureGame lastSnapshot,
      OpMove existingPlan) {
    return !Objects.equals(newSnapshot, lastSnapshot);
  }

  /**
   * Error hook for failed computations.
   *
   * <p>Default behavior unwraps {@link CompletionException}, ignores cancellations,
   * re-asserts interrupt status for {@link InterruptedException}, and logs an error.</p>
   *
   * @param throwable the failure thrown by the async computation
   */
  protected void onError(Throwable throwable) {
    if (throwable instanceof CompletionException ce && ce.getCause() != null) {
      throwable = ce.getCause();
    }
    if (throwable instanceof CancellationException) {
      return; // expected under churn
    } else if (throwable instanceof InterruptedException) {
      Thread.currentThread().interrupt();
      return;
    }
    LOG.log(Level.SEVERE, "AI computation failed", throwable);
  }

  /**
   * Submits an asynchronous computation to derive an {@link OpMove} plan for the given snapshot.
   *
   * <p><strong>Contract:</strong>
   * <ul>
   *   <li>Must not mutate {@code snapshot}.</li>
   *   <li>Should be responsive to interruption if long-running.</li>
   *   <li>Must return a non-{@code null} {@link CompletableFuture} (may complete exceptionally).
   *   </li>
   * </ul>
   * </p>
   *
   * @param snapshot the immutable game state to plan against
   *
   * @return a future that completes with the planned move or completes exceptionally
   */
  protected abstract CompletableFuture<OpMove> submit(PureGame snapshot);

  /**
   * Computes the immediate {@link GameIntent} to move the active piece toward the given plan.
   *
   * <p>This default implementation is a simple controller:
   * <ol>
   *   <li>If {@link #needsRotation(OpMove, PureGame)} is {@code true}, returns {@link
   *   GameIntent#ROTATE_CW}.</li>
   *   <li>Otherwise compares {@link #getCurrentX(PureGame)} with {@code plan.opX()} and
   *       returns {@link GameIntent#MOVE_LEFT} or {@link GameIntent#MOVE_RIGHT} as needed.</li>
   *   <li>If aligned horizontally, delegates to {@link #whenAligned(OpMove, PureGame)} (default
   *   {@code null}).</li>
   * </ol>
   * </p>
   *
   * @param opMove   the latest completed plan (maybe {@code null})
   * @param snapshot the current snapshot (maybe {@code null})
   *
   * @return the next intent or {@code null} if no action is needed
   */
  protected GameIntent getCurrentIntent(OpMove opMove, PureGame snapshot) {
    if (opMove == null || snapshot == null) {
      return null;
    }
    if (snapshot.tetromino() == null) {
      return null;
    }
    if (needsRotation(opMove, snapshot)) {
      return GameIntent.ROTATE_CW;
    }
    int currentX = getCurrentX(snapshot);
    int targetX = opMove.opX();
    if (currentX < targetX) {
      return GameIntent.MOVE_RIGHT;
    }
    if (currentX > targetX) {
      return GameIntent.MOVE_LEFT;
    }
    return whenAligned(opMove, snapshot);
  }

  /**
   * Hook to decide whether the piece still needs rotation before horizontal alignment.
   *
   * <p>Default: returns {@code plan.opRotate() != 0} (i.e., any remaining rotation count).</p>
   *
   * @param opMove   the planned move
   * @param snapshot the current snapshot
   *
   * @return {@code true} if a rotation intent should be emitted this tick
   */
  protected boolean needsRotation(OpMove opMove, PureGame snapshot) {
    return opMove.opRotate() != 0;
  }

  /**
   * Hook to provide the current piece's reference X position used for horizontal alignment.
   *
   * <p>Default: uses the tetromino's pivot X, i.e. {@code snapshot.tetromino().position().x()}.
   * Subclasses may override to use, for example, the left bound ({@code minX}) in board
   * coordinates.</p>
   *
   * @param snapshot the current snapshot
   *
   * @return the current X used by the controller to compare against {@code plan.opX()}
   */
  protected int getCurrentX(PureGame snapshot) {
    return snapshot.tetromino().position().x();
  }

  /**
   * Hook invoked when rotation is satisfied and the piece is horizontally aligned with the target.
   *
   * <p>Default: returns {@code null} (no action). Subclasses may return
   * {@link GameIntent#HARD_DROP}, {@link GameIntent#SOFT_DROP} (soft drop), or another action.</p>
   *
   * @param opMove   the planned move
   * @param snapshot the current snapshot
   *
   * @return the intent to emit when aligned, or {@code null} to do nothing
   */
  protected GameIntent whenAligned(OpMove opMove, PureGame snapshot) {
    return null;
  }

  /**
   * Releases resources held by the AI (e.g., executor threads, native handles).
   *
   * <p>Called when the AI is no longer needed. Implementations should be idempotent.</p>
   */
  public abstract void shutdown();
}
