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
 * Base class for Tetris AIs that compute a planned move ({@link OpMove}) for an immutable game
 * snapshot ({@link PureGame}) and emit a {@link GameIntent} each tick to steer the active tetromino
 * toward that plan.
 *
 * <h2>Planning model</h2>
 * <ul>
 *   <li>Each distinct {@link PureGame} snapshot may trigger an asynchronous planning task
 *       via {@link #submit(PureGame)}.</li>
 *   <li>Only one plan computation is considered “current” (the most recently submitted future).
 *       Older futures are ignored on completion.</li>
 *   <li>The last completed plan is cached and used by {@link #getCurrentIntent(OpMove, PureGame)}
 *       to emit one {@link GameIntent} per tick until a replan happens.</li>
 * </ul>
 *
 * <h2>Default controller</h2>
 * The default {@link #getCurrentIntent(OpMove, PureGame)} implements a simple follow-the-plan
 * controller:
 * <ol>
 *   <li>Rotate until {@link #needsRotation(OpMove, PureGame)} is false (defaults to
 *       {@code plan.opRotate() == 0}).</li>
 *   <li>Move horizontally toward {@code plan.opX()} using {@link #getCurrentX(PureGame)} as the
 *       reference X (defaults to the piece pivot X).</li>
 *   <li>When aligned, delegate to {@link #whenAligned(OpMove, PureGame)} (default: {@code null}).
 *   </li>
 * </ol>
 * Subclasses can override these hooks to implement smarter controllers (e.g., wall kicks,
 * DAS/ARR simulation, soft-drop timing, etc.).
 *
 * <h2>Replan policy</h2>
 * The default {@link #shouldStartNewPlan(PureGame, PureGame, OpMove)} returns {@code true}
 * whenever {@code !Objects.equals(newSnapshot, lastSnapshot)}. Override to replan only on piece
 * spawn, on interval, or when the existing plan becomes invalid.
 *
 * <h2>Concurrency &amp; cancellation</h2>
 * <ul>
 *   <li>At most one planning computation is considered active. When a new plan is started,
 *       any in-flight computation is canceled ( the best effort) via
 *       {@link CompletableFuture#cancel(boolean)}.</li>
 *   <li>Field access uses {@code volatile} for visibility; external synchronization is not
 *   required.</li>
 *   <li>{@link #submit(PureGame)} should be responsive to interruption if long-running.</li>
 *   <li>Completion exceptions are unwrapped by {@link #onError(Throwable)}; cancellations are
 *   treated as expected churn.</li>
 * </ul>
 *
 * <h2>Contracts &amp; expectations</h2>
 * <ul>
 *   <li>{@code PureGame} is immutable; implementations of {@link #submit(PureGame)} must not mutate
 *   it.</li>
 *   <li>{@link OpMove#opX()} is interpreted by the default controller as a target X (pivot-aligned
 *       by default; override {@link #getCurrentX(PureGame)} if you align differently).</li>
 *   <li>{@link OpMove#opRotate()} is the remaining clockwise rotation count required to reach the
 *   plan.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * Call {@link #shutdown()} when the AI is no longer needed so implementations can release
 * executors or native resources. This method should be idempotent.
 */
public abstract class Ai {

  /**
   * Logger for AI diagnostics and failure reporting.
   */
  protected static final Logger LOG = Logger.getLogger(Ai.class.getName());

  /**
   * Last snapshot we planned for. Used to decide whether a new plan is required.
   */
  private volatile PureGame cachePureGame = null;

  /**
   * The last completed plan for {@link #cachePureGame}. May be {@code null} while planning or if
   * planning failed.
   */
  private volatile OpMove cachedOpMove = null;

  /**
   * The current async plan computation, if any (the only one considered “current”).
   */
  private volatile CompletableFuture<OpMove> inFlight;

  /**
   * Advances the AI by one tick against the provided immutable {@link PureGame} snapshot.
   *
   * <p>Behavior:</p>
   * <ul>
   *   <li>If {@code snapshot} differs from the last planned snapshot per
   *       {@link #shouldStartNewPlan(PureGame, PureGame, OpMove)}, cancel any in-flight
   *       computation, clear the cached plan, and submit a new computation via
   *       {@link #submit(PureGame)}.</li>
   *   <li>If a plan has completed, emit a {@link GameIntent} that moves/rotates the current piece
   *       toward that plan by delegating to {@link #getCurrentIntent(OpMove, PureGame)}.</li>
   *   <li>If no plan is available yet (or there is no active piece), returns {@code null}.</li>
   * </ul>
   *
   * @param snapshot the current immutable game state; {@code null} yields {@code null}
   *
   * @return the next intent for this frame, or {@code null} if no action is required or no plan is
   *     ready
   */
  public final GameIntent tick(PureGame snapshot) {
    if (snapshot == null) {
      return null;
    }
    PureGame lastPureGame = cachePureGame;
    cachePureGame = snapshot;

    if (shouldStartNewPlan(cachePureGame, lastPureGame, cachedOpMove)) {
      // Clear/abort previous work
      cachedOpMove = null;
      if (inFlight != null && !inFlight.isDone()) {
        inFlight.cancel(true);
      }
      inFlight = null;

      // Submit new work
      final CompletableFuture<OpMove> future = submit(snapshot);
      if (future == null) {
        onError(new NullPointerException("Ai.submit() returned null"));
        return null;
      }

      inFlight = future;
      future.whenComplete((opMove, throwable) -> {
        // Ignore if superseded by a newer submission.
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
   * <p>Default: replan when {@code !Objects.equals(newSnapshot, lastSnapshot)}.</p>
   *
   * <p>Common overrides include:</p>
   * <ul>
   *   <li>Replan only on new piece spawn (compare active piece identity/type).</li>
   *   <li>Throttle replans (time- or frame-based).</li>
   *   <li>Force replan when the current plan conflicts with the new board geometry.</li>
   * </ul>
   * Keep this decision fast and side effect free; it runs every frame.
   *
   * @param newSnapshot  the current snapshot (non-null)
   * @param lastSnapshot the snapshot associated with {@code existingPlan} (maybe null)
   * @param existingPlan the last completed plan (maybe null)
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
   * <p>Default behavior:</p>
   * <ul>
   *   <li>Unwraps {@link CompletionException} to its cause when present.</li>
   *   <li>Silently ignores {@link CancellationException} (expected under churn).</li>
   *   <li>Preserves interrupt status for {@link InterruptedException} and returns.</li>
   *   <li>Logs at {@link Level#SEVERE} for all other failures.</li>
   * </ul>
   *
   * @param throwable the failure thrown by the async computation (never null)
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
   * <p><strong>Contract:</strong></p>
   * <ul>
   *   <li>Must not mutate {@code snapshot}.</li>
   *   <li>Should be responsive to interruption (cooperate with {@code cancel(true)}).</li>
   *   <li>Must return a non-{@code null} {@link CompletableFuture} that may complete normally
   *       or exceptionally.</li>
   * </ul>
   *
   * @param snapshot the immutable game state to plan against
   *
   * @return a future that completes with the planned move (maybe {@code null} if no move
   *     available), or completes exceptionally
   */
  protected abstract CompletableFuture<OpMove> submit(PureGame snapshot);

  /**
   * Computes the immediate {@link GameIntent} to move the active piece toward the given plan.
   *
   * <p>Default controller:</p>
   * <ol>
   *   <li>If {@link #needsRotation(OpMove, PureGame)} is {@code true}, returns
   *       {@link GameIntent#ROTATE_CW}.</li>
   *   <li>Otherwise compares {@link #getCurrentX(PureGame)} with {@code plan.opX()} and
   *       returns {@link GameIntent#MOVE_LEFT} / {@link GameIntent#MOVE_RIGHT} as needed.</li>
   *   <li>If aligned horizontally, delegates to {@link #whenAligned(OpMove, PureGame)}
   *       (default: {@code null}).</li>
   * </ol>
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
   * <p>Default: {@code plan.opRotate() != 0}.</p>
   *
   * @param opMove   the planned move (non-null in default flow)
   * @param snapshot the current snapshot (non-null in default flow)
   *
   * @return {@code true} if a rotation intent should be emitted this tick
   */
  protected boolean needsRotation(OpMove opMove, PureGame snapshot) {
    return opMove.opRotate() != 0;
  }

  /**
   * Hook to provide the current piece's reference X used for horizontal alignment.
   *
   * <p>Default: the tetromino's pivot X,
   * {@code snapshot.tetromino().position().x()}. Override to align to another reference (e.g.,
   * left bound/minX).</p>
   *
   * @param snapshot the current snapshot
   *
   * @return the current X used to compare against {@code plan.opX()}
   */
  protected int getCurrentX(PureGame snapshot) {
    return snapshot.tetromino().position().x();
  }

  /**
   * Hook invoked when rotation is satisfied and the piece is horizontally aligned with the target.
   *
   * <p>Default: {@code null} (no action). Common overrides include:
   * {@link GameIntent#HARD_DROP}, {@link GameIntent#SOFT_DROP}, or a micro-move for finesse.</p>
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
