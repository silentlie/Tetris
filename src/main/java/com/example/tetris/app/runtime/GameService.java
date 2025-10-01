package com.example.tetris.app.runtime;

import com.example.tetris.core.rule.Game;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.animation.AnimationTimer;

/**
 * Drives one or more {@link Game} instances using JavaFX's {@link AnimationTimer}.
 *
 * <p>Each frame, {@link #handle(long)} computes the nanosecond delta since the previous frame
 * and forwards it to every registered game via {@link Game#tick(long)}. When all games report
 * game-over, the service stops itself and invokes the {@code onAllFinished} callback.</p>
 *
 * <p>Lifecycle is controlled via {@link #start()} and {@link #stop()}. The service will not
 * start if no games are registered.</p>
 *
 * <p>Thread-safety: must be used from the JavaFX application thread.</p>
 */
public final class GameService extends AnimationTimer {

  /**
   * Managed games advanced each frame.
   */
  private final List<Game> games = new ArrayList<>();

  /**
   * Callback fired once when all games have finished.
   */
  private final Runnable onAllFinished;

  /**
   * Timestamp of the previous frame (in nanoseconds), as provided by {@link #handle(long)}.
   */
  private long last = 0L;

  /**
   * Whether the timer is currently running.
   */
  private boolean isRunning = false;

  /**
   * Creates a new service with a completion callback.
   *
   * @param onAllFinished callback invoked after the frame in which all games become over (must not
   *                      be {@code null})
   */
  public GameService(Runnable onAllFinished) {
    this.onAllFinished = Objects.requireNonNull(onAllFinished, "onAllFinished is null");
  }

  /**
   * Adds a game to be driven by this service.
   *
   * <p>Games are ticked in insertion order once per frame.</p>
   *
   * @param g the game to add (must not be {@code null})
   */
  public void addGame(Game g) {
    games.add(g);
  }

  /**
   * Returns whether the underlying {@link AnimationTimer} is running.
   *
   * @return {@code true} if running; {@code false} otherwise
   */
  public boolean isRunning() {
    return isRunning;
  }

  /**
   * JavaFX animation callback. Computes the time delta and advances all games.
   *
   * <p>On the very first invocation after {@link #start()}, the method initializes the
   * {@link #last} timestamp and returns without ticking, so that subsequent frames have a
   * meaningful delta.</p>
   *
   * @param now current timestamp in nanoseconds (monotonic), provided by JavaFX
   */
  @Override
  public void handle(long now) {
    if (last == 0L) {
      last = now;
      return;
    }
    long dt = now - last;
    last = now;

    boolean allOver = !games.isEmpty();
    for (Game g : games) {
      g.tick(dt);
      if (!g.isGameOver()) {
        allOver = false;
      }
    }
    if (allOver) {
      stop();
      onAllFinished.run();
    }
  }

  /**
   * Starts the animation loop if there is at least one game.
   *
   * <p>Resets internal timing so the next {@link #handle(long)} call establishes the baseline.</p>
   */
  @Override
  public void start() {
    if (games.isEmpty()) {
      return;
    }
    isRunning = true;
    last = 0L;
    super.start();
  }

  /**
   * Stops the animation loop and resets internal timing state.
   */
  @Override
  public void stop() {
    isRunning = false;
    last = 0L;
    super.stop();
  }
}
