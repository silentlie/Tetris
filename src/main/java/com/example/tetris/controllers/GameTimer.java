package com.example.tetris.controllers;

import static com.example.tetris.models.Game.K;

import com.example.tetris.models.Game;
import javafx.animation.AnimationTimer;

/**
 * Abstract timer for controlling the automatic fall speed and game loop updates in the Tetris
 * game.
 *
 * <p>Extends {@link AnimationTimer} to provide a consistent per-frame update loop.
 * Manages fall intervals that decrease as play time increases (difficulty scaling).
 * </p>
 *
 * <p>Subclasses must implement the abstract callbacks to handle falling,
 * locking, rendering, ending the game, and marking the board as dirty.
 * </p>
 */
public abstract class GameTimer extends AnimationTimer {

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

  /**
   * Called automatically by the JavaFX runtime for each frame.
   *
   * @param now current timestamp in nanoseconds
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
      if (!onFallStep()) {
        if (onLock()) {
          onEndGame();
          return;
        }
        // reset if lock happened
        accNanos = 0L;
      }
      onDirty();
    }
    onRender();
  }

  /**
   * Called each time a fall step is executed (piece moves down by one row).
   *
   * @return true if piece successfully moved down, false if it hit the bottom or another piece
   */
  protected abstract boolean onFallStep();

  /**
   * Called when a piece can no longer fall and must be locked into the board.
   *
   * @return true if locking ends the game, false otherwise
   */
  protected abstract boolean onLock();

  /**
   * Called when the game ends, either due to a lock-out condition or loss.
   */
  protected abstract void onEndGame();

  /**
   * Called every frame to render the current state of the game.
   */
  protected abstract void onRender();

  /**
   * Called when the game state changes and requires a re-render.
   */
  protected abstract void onDirty();

  /**
   * Stops the timer and resets counters.
   */
  @Override
  public void stop() {
    last = 0L;
    accNanos = 0L;
    isRunning = false;
    super.stop();
  }

  /**
   * Starts the timer.
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
   * Calculates the current fall interval (speed of piece falling) based on elapsed time, increasing
   * difficulty over time.
   *
   * <ol>
   *   <li>Converts nanoseconds to seconds</li>
   *   <li>Computes the current level based on a continuous curve</li>
   *   <li>Clamps the level to the maximum {@link Game#K}</li>
   *   <li>Determines seconds per row fall interval</li>
   *   <li>Converts seconds back to nanoseconds</li>
   * </ol>
   *
   * @param elapsedNanos total elapsed nanoseconds since game start
   * @return fall interval in nanoseconds
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
