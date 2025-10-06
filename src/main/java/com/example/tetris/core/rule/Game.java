package com.example.tetris.core.rule;

import com.example.tetris.ai.Ai;
import com.example.tetris.core.state.GameIntent;
import com.example.tetris.core.state.GameState;
import com.example.tetris.ui.view.GameView;
import java.time.Duration;
import java.util.Objects;

/**
 * Orchestrates a single running game: advances time/gravity, applies inputs (optionally via an AI),
 * and renders the current frame.
 *
 * <p>This class wires together the core model ({@link GameState}), the timing/level curve
 * ({@link GameClock}), and the UI surface ({@link GameView}). Typical usage calls
 * {@link #tick(long)} from an animation timer with the frame delta in <em>nanoseconds</em>, and
 * sets an {@link Ai} via {@link #ai(Ai)} to drive automated intents (or applies intents elsewhere
 * in response to input events).</p>
 *
 * <h2>Invocation contract</h2>
 * <ul>
 *   <li>Call {@link #tick(long)} once per frame with the delta in nanoseconds (e.g., from
 *       {@code AnimationTimer}). Negative values are treated as zero.</li>
 *   <li>Attach an {@link Ai} with {@link #ai(Ai)} to emit an intent each frame (optional).</li>
 *   <li>Rendering occurs every call to {@link #tick(long)}, even after game over.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 *
 * <p><strong>Not thread-safe.</strong> Call all methods from the JavaFX application thread.</p>
 *
 * <h2>Order of operations per tick</h2>
 * <ol>
 *   <li>Clamp {@code dtNanos} to {@code >= 0} and advance {@link GameClock}.</li>
 *   <li>Accumulate gravity time; while accumulator ≥ {@link GameClock#fallIntervalNanos()},
 *       subtract one interval and apply {@link GameIntent#SOFT_DROP}. If state changed, mark the
 *       view dirty.</li>
 *   <li>If an {@link Ai} is present, compute an intent from {@code state.toPureGame()} and apply
 *       it immediately if non-{@code null}.</li>
 *   <li>Render via {@link GameView#render(GameState, GameClock)}.</li>
 * </ol>
 *
 * @see GameClock
 * @see GameState
 * @see GameIntent
 * @see GameView
 * @see Ai
 * @since 1.0
 */
public class Game {

  /**
   * Mutable game state model for rules, movement, scoring, and piece lifecycle.
   */
  private final GameState state;

  /**
   * Surface/adapter that renders {@link GameState} and tracks dirtiness.
   */
  private final GameView view;

  /**
   * Level curve and fall-interval timing advanced by {@link #tick(long)}.
   */
  private final GameClock clock;

  /**
   * Optional AI used to emit control intents that steer the active tetromino.
   */
  private Ai ai;

  /**
   * Accumulator for gravity timing in nanoseconds. When this meets or exceeds
   * {@link GameClock#fallIntervalNanos()}, a soft drop is applied and the accumulator is reduced.
   */
  private long gravityAccNanos = 0L;

  /**
   * Creates a controller for a single game run.
   *
   * @param state the mutable game state (must not be {@code null})
   * @param view  the view responsible for rendering and dirty tracking (must not be {@code null})
   * @param clock the timing/level curve (must not be {@code null})
   *
   * @throws NullPointerException if any argument is {@code null}
   */
  public Game(GameState state, GameView view, GameClock clock) {
    this.state = Objects.requireNonNull(state, "state is null");
    this.view = Objects.requireNonNull(view, "view is null");
    this.clock = Objects.requireNonNull(clock, "clock is null");
  }

  /**
   * Attaches an {@link Ai} to steer the game each tick.
   *
   * <p>The AI is consulted during {@link #tick(long)} after gravity processing. If it returns a
   * non-{@code null} {@link GameIntent}, that intent is applied immediately via
   * {@code state.apply(intent)}.</p>
   *
   * <p>Passing {@code null} is not permitted and will throw.</p>
   *
   * @param ai the AI to attach (must not be {@code null})
   *
   * @throws NullPointerException if {@code ai} is {@code null}
   */
  public void ai(Ai ai) {
    this.ai = Objects.requireNonNull(ai, "ai is null");
  }

  /**
   * Applies a single intent to the underlying {@link GameState}.
   *
   * @param intent the intent to apply (must not be {@code null})
   *
   * @throws NullPointerException if {@code intent} is {@code null}
   */
  public void apply(GameIntent intent) {
    if (intent == null) {
      return;
    }
    if (state.apply(intent)) {
      view.markDirty();
    }
  }

  /**
   * Returns the total elapsed wall-clock time tracked by the {@link GameClock}.
   *
   * @return elapsed time since start of the run (never {@code null})
   */
  public Duration elapsed() {
    return Duration.ofNanos(clock.elapsedNanos());
  }

  /**
   * Returns the current score from the underlying {@link GameState}.
   *
   * @return the score value
   */
  public int score() {
    return state.getScore();
  }

  /**
   * Returns the highest consecutive line-clear streak achieved in this run.
   *
   * @return the maximum streak
   */
  public int highestStreak() {
    return state.getHighestStreak();
  }

  /**
   * Returns whether the game can no longer accept inputs (lock-out or post-spawn collision).
   *
   * @return {@code true} if the game has ended; {@code false} otherwise
   */
  public boolean isGameOver() {
    return state.isGameOver();
  }

  /**
   * Advances time, applies gravity steps as needed, executes optional AI, and renders the frame.
   *
   * <p><strong>Behavior:</strong></p>
   * <ul>
   *   <li>If the game is not over, advance {@link GameClock} by {@code dtNanos}
   *       (negative values are treated as zero).</li>
   *   <li>Accumulate {@code dtNanos}. While the accumulator is ≥
   *       {@link GameClock#fallIntervalNanos()} and the game is not over, subtract one interval
   *       and apply {@link GameIntent#SOFT_DROP}. If the state changed, call
   *       {@link GameView#markDirty()}.</li>
   *   <li>If an {@link Ai} is attached, obtain an intent using
   *       {@code ai.tick(state.toPureGame())} and apply it if non-{@code null}.</li>
   *   <li>Render the current state via {@link GameView#render(GameState, GameClock)} (always).</li>
   * </ul>
   *
   * @param dtNanos elapsed time since the previous frame in nanoseconds; negatives are ignored
   *                (treated as zero)
   *
   * @implNote Gravity accumulation is monotonic per tick; very large {@code dtNanos} can
   *     trigger multiple soft-drop steps in one call.
   */
  public void tick(long dtNanos) {
    if (!state.isGameOver()) {
      if (dtNanos < 0) {
        dtNanos = 0;
      }
      clock.tick(dtNanos);
      gravityAccNanos += dtNanos;

      while (gravityAccNanos >= clock.fallIntervalNanos() && !state.isGameOver()) {
        gravityAccNanos -= clock.fallIntervalNanos();
        if (state.apply(GameIntent.SOFT_DROP)) {
          view.markDirty();
        }
      }

      if (ai != null) {
        GameIntent intent = ai.tick(state.toPureGame());
        if (intent != null && state.apply(intent)) {
          view.markDirty();
        }
      }
    }
    view.render(state, clock);
  }
}
