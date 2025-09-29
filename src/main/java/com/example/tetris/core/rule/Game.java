package com.example.tetris.core.rule;

import com.example.tetris.core.state.GameIntent;
import com.example.tetris.core.state.GameState;
import com.example.tetris.ui.view.GameView;
import java.util.Objects;

/**
 * Orchestrates a single running game: advances time/gravity, applies inputs, and renders.
 *
 * <p>This class wires together the core model ({@link GameState}), the timing/level curve
 * ({@link GameClock}), and the UI surface ({@link GameView}). The typical loop calls
 * {@link #tick(long)} from an animation timer with the frame delta in nanoseconds, and calls
 * {@link #apply(GameIntent)} in response to input events.</p>
 *
 * <p>Thread-safety: not thread-safe. Call all methods from the JavaFX application thread.</p>
 */
public class Game {

  private final GameState state;
  private final GameView view;
  private final GameClock clock;

  /**
   * Accumulator for gravity timing in nanoseconds. When this exceeds
   * {@link GameClock#fallIntervalNanos()}, a soft drop is applied and the accumulator is reduced.
   */
  private long gravityAccNanos = 0L;

  /**
   * Creates a controller for a single game run.
   *
   * @param state the mutable game state (must not be {@code null})
   * @param view  the view responsible for rendering and dirty tracking (must not be {@code null})
   * @param clock the timing/level curve (must not be {@code null})
   */
  public Game(GameState state, GameView view, GameClock clock) {
    this.state = Objects.requireNonNull(state, "state is null");
    this.view = Objects.requireNonNull(view, "view is null");
    this.clock = Objects.requireNonNull(clock, "clock is null");
  }

  /**
   * Applies a high-level input intent to the game.
   *
   * <p>If the game is already over, the intent is ignored and {@code false} is returned.
   * Otherwise, the intent is forwarded to {@link GameState#apply(GameIntent)}. When the state
   * changes, the view is marked dirty to trigger a re-render on the next {@link #tick(long)}.</p>
   *
   * @param intent the input to process
   *
   * @return {@code true} if the intent was processed by the state; {@code false} if ignored
   */
  public boolean apply(GameIntent intent) {
    if (isGameOver()) {
      return false;
    }
    if (state.apply(intent)) {
      view.markDirty();
      return true;
    }
    return false;
  }

  /**
   * Returns whether the game can no longer accept inputs (lock-out or post-spawn collision).
   *
   * @return {@code true} if the game has ended; otherwise {@code false}
   */
  public boolean isGameOver() {
    return state.isGameOver();
  }

  /**
   * Advances time, applies gravity steps as needed, and renders the frame.
   *
   * <p>Behavior:</p>
   * <ul>
   *   <li>If the game is not over, advance {@link GameClock} by {@code dtNanos}.</li>
   *   <li>Accumulate {@code dtNanos} into {@link #gravityAccNanos}. While the accumulator is
   *       ≥ {@link GameClock#fallIntervalNanos()}, subtract one interval and apply a
   *       {@link GameIntent#SOFT_DROP}. Mark the view dirty if the state changed.</li>
   *   <li>Render the current state via {@link GameView} (concrete signature  as defined in your
   *   view).</li>
   * </ul>
   *
   * <p>Call this once per frame from your animation loop. Pass the frame delta in nanoseconds
   * (e.g., from {@code AnimationTimer}).</p>
   *
   * @param dtNanos elapsed time since the previous frame in nanoseconds (maybe 0; negative
   *                ignored)
   */
  public void tick(long dtNanos) {
    if (!isGameOver()) {
      clock.tick(dtNanos);
      gravityAccNanos += dtNanos;
      while (gravityAccNanos >= clock.fallIntervalNanos() && !state.isGameOver()) {
        gravityAccNanos -= clock.fallIntervalNanos();
        if (state.apply(GameIntent.SOFT_DROP)) {
          view.markDirty();
        }
      }
    }
    view.render(state, clock);
  }
}
