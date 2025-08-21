package com.example.tetris.helpers;

import javafx.animation.AnimationTimer;

/**
 * Base class for managing a fixed-step game loop using JavaFX's {@link AnimationTimer}.
 *
 * <p>Provides timing utilities such as accumulated nanoseconds since the last update,
 * total elapsed time, and a running flag for pause/resume logic.
 * </p>
 *
 * <p>Subclasses should implement {@link #gameLoop()} to update game state each frame.
 * </p>
 */
public abstract class GameTimer extends AnimationTimer {

  /**
   * Last timestamp recorded in nanoseconds.
   *
   * <p>Used to calculate frame-to-frame deltas.
   * </p>
   */
  protected long last = 0L;
  /**
   * Accumulated nanoseconds since the last fixed-step update.
   *
   * <p>Typically consumed by gravity or physics calculations that run
   * at intervals rather than every frame.
   * </p>
   */
  protected long accumulateNanos = 0L;
  /**
   * Total elapsed nanoseconds since the timer started.
   *
   * <p>Preserved across pause/resume so difficulty scaling or
   * time-based logic continues smoothly.
   * </p>
   */
  protected long elapsedNanos = 0L;
  /**
   * Whether the timer is currently active.
   *
   * <p>Provides an easy check for pause/resume state in controllers.
   * </p>
   */
  protected boolean isRunning = false;

  /**
   * Called once per frame after time calculations are updated.
   *
   * <p>Subclasses should implement this method to apply game logic (e.g. gravity, collision,
   * rendering).
   * </p>
   */
  protected abstract void gameLoop();

  /**
   * Internal handler invoked each frame by JavaFX.
   *
   * <p>Computes the delta time since the last frame, updates
   * {@link #elapsedNanos} and {@link #accumulateNanos}, then delegates to {@link #gameLoop()}.
   * </p>
   *
   * @param now the current timestamp in nanoseconds
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
    accumulateNanos += delta;
    gameLoop();
  }

  /**
   * Stops the timer and resets per-frame accumulators ({@link #last}, {@link #accumulateNanos}).
   *
   * <p><strong>Note:</strong> {@link #elapsedNanos} is intentionally preserved
   * so that time-based logic (e.g. gravity/difficulty) is unaffected when pausing and
   * resuming.</p>
   */
  @Override
  public void stop() {
    last = 0L;
    accumulateNanos = 0L;
    isRunning = false;
    super.stop();
  }

  /**
   * Starts ticking on the JavaFX Application Thread.
   *
   * <p>Also sets {@link #isRunning} to {@code true} for convenience.
   * </p>
   */
  @Override
  public void start() {
    isRunning = true;
    super.start();
  }
}
