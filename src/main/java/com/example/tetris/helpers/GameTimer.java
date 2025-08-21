package com.example.tetris.helpers;

import static com.example.tetris.models.Game.K;

import javafx.animation.AnimationTimer;

/**
 * Frame timer that drives automatic falling and delegates actions via {@link GameLoopCallbacks}.
 *
 * <p>Built on {@link javafx.animation.AnimationTimer} so ticks occur on the JavaFX
 * Application Thread. Maintains an accumulated time budget and executes one or more "fall steps"
 * per frame based on a decreasing fall interval (difficulty curve).</p>
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>Accumulate elapsed time since the last frame.</li>
 *   <li>While accumulated time ≥ fall interval:
 *     <ol>
 *       <li>Invoke {@link GameLoopCallbacks#softDrop()}.</li>
 *       <li>If blocked, invoke {@link GameLoopCallbacks#lock()} and, if it returns {@code true},
 *           call {@link GameLoopCallbacks#endGame()} and stop processing this frame.</li>
 *       <li><em>Implementations</em> of these callbacks should call
 *       {@link GameLoopCallbacks#markDirty()} whenever they mutate state so rendering can be gated
 *       efficiently.</li>
 *     </ol>
 *   </li>
 *   <li>Call {@link GameLoopCallbacks#render()} once per frame.</li>
 * </ol>
 *
 * <p>The fall interval shortens over time using a continuous level curve clamped by {@code Game.K}.
 * {@code K} must be ≥ 2 to avoid division by zero in the curve.</p>
 */
public class GameTimer extends AnimationTimer {

  /**
   * Last timestamp recorded (nanoseconds).
   */
  private long last = 0L;
  /**
   * Accumulated nanoseconds since last fall step.
   */
  private long accNanos = 0L;
  /**
   * Total elapsed nanoseconds since the game started.
   */
  private long elapsedNanos = 0L;
  /**
   * Tracks whether the timer is actively running.
   */
  private boolean isRunning = false;

  private final GameLoopCallbacks cb;

  /**
   * Create a new timer that delegates game actions to the provided callbacks.
   *
   * @param callbacks recipient of fall/lock/end/render/dirty events
   */
  public GameTimer(GameLoopCallbacks callbacks) {
    this.cb = callbacks;
  }

  /**
   * Per-frame tick called by the JavaFX runtime with a monotonic time source (nanoseconds).
   *
   * @param now current timestamp in nanoseconds (monotonic)
   */
  @Override
  public void handle(long now) {
    if (last == 0L) {
      last = now;
      return;
    }
    long delta = now - last;
    last = now;
    elapsedNanos += delta;
    long fallInterval = getFallIntervalNanos(elapsedNanos);
    accNanos += delta;
    // Perform one or more fall steps depending on accumulated time
    while (accNanos >= fallInterval) {
      accNanos -= fallInterval;
      if (cb.softDrop()) {
        continue;
      }
      if (cb.lock()) {
        cb.endGame();
        return;
      }
      // reset if lock happened
      accNanos = 0L;
    }
    cb.render();
  }

  /**
   * Stops ticking and resets per-frame accumulators ({@code last}, {@code accNanos}).
   *
   * <p><strong>Note:</strong> {@code elapsedNanos} is intentionally preserved so that
   * gravity/difficulty is unchanged across pause/resume.</p>
   */
  @Override
  public void stop() {
    last = 0L;
    accNanos = 0L;
    isRunning = false;
    super.stop();
  }

  /**
   * Starts ticking on the JavaFX Application Thread. Sets an internal running flag for easy checks
   * in controllers.
   */
  @Override
  public void start() {
    isRunning = true;
    super.start();
  }

  /**
   * Checks if the timer is currently running.
   *
   * @return true if running, false otherwise
   */
  public boolean isRunning() {
    return isRunning;
  }

  /**
   * Computes the current fall interval (nanoseconds per row) from total elapsed time.
   *
   * <p>Steps:</p>
   * <ol>
   *   <li>Convert elapsed nanoseconds to seconds.</li>
   *   <li>Compute a continuous "level" that increases with time.</li>
   *   <li>Clamp level to {@code [1, K]}.</li>
   *   <li>Map level to seconds-per-row using an exponential decay.</li>
   *   <li>Convert seconds back to nanoseconds.</li>
   * </ol>
   *
   * @param elapsedNanos total elapsed time since the timer started (or since last reset)
   *
   * @return fall interval in nanoseconds (never zero)
   */
  private long getFallIntervalNanos(long elapsedNanos) {
    // 1. Convert nanos -> seconds (as double for precision)
    double elapsedSeconds = elapsedNanos / 1_000_000_000.0;

    // 2. Continuous level curve
    double level = (1.0 + Math.sqrt(1.0 + (2.0 * elapsedSeconds) / 15.0)) / 2.0;

    // 3. Clamp so we don’t exceed max K levels
    double clampedLevel = Math.min(level, K);

    // 4. Seconds per row
    double secondsPerRow = Math.pow(1.0 / 30.0, (clampedLevel - 1.0) / (K - 1));

    // 5. Convert back to nanos
    return (long) (secondsPerRow * 1_000_000_000.0);
  }
}
