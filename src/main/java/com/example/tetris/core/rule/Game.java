package com.example.tetris.core.rule;

import com.example.tetris.ai.Ai;
import com.example.tetris.core.state.GameIntent;
import com.example.tetris.core.state.GameState;
import com.example.tetris.ui.view.GameView;
import java.util.Objects;

/**
 * Orchestrates a single running game: advances time/gravity, applies inputs, optionally steers via
 * an AI plan, and renders the current frame.
 *
 * <p>This class wires together the core model ({@link GameState}), the timing/level curve
 * ({@link GameClock}), and the UI surface ({@link GameView}). Typical usage calls
 * {@link #tick(long)} from an animation timer with the frame delta in nanoseconds, and calls
 * {@link #apply(GameIntent)} in response to input events.</p>
 *
 * <ul>
 *   <li>Call {@link #tick(long)} once per frame with the delta in nanoseconds (e.g., from
 *       {@code AnimationTimer}).</li>
 *   <li>Call {@link #apply(GameIntent)} in response to input events.</li>
 * </ul>
 *
 * <p><strong>Threading:</strong> not thread-safe. Call all methods from the JavaFX application
 * thread.</p>
 */
public class Game {

  /**
   * The mutable game state model for rules, movement, scoring, and piece lifecycle.
   */
  private final GameState state;

  /**
   * The surface/adapter that renders {@link GameState} and tracks dirtiness.
   */
  private final GameView view;

  /**
   * Provides the level curve and fall interval timing; advanced each frame by {@link #tick(long)}.
   */
  private final GameClock clock;
  private Listener listener = new Listener() {
  };

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
   * Create a controller for a single game run.
   *
   * <p>Installs internal bridges so that:</p>
   * <ul>
   *   <li>{@link GameClock.Listener#onLevelUp(int)} is forwarded to
   *   {@link Listener#onLevelUp(int)}.</li>
   *   <li>{@link GameState.Listener#onGameOver()} is forwarded to {@link Listener#onGameOver()}.
   *   </li>
   *   <li>{@link GameState.Listener#onClearLines(int, int, int, int)} is forwarded to
   *       {@link Listener#onClearLines(int, int, int, int)}.</li>
   * </ul>
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
    clock.setListener(new GameClock.Listener() {
      @Override
      public void onLevelUp(int level) {
        listener.onLevelUp(level);
      }
    });
    state.setListener(new GameState.Listener() {
      @Override
      public void onGameOver() {
        listener.onGameOver();
      }

      @Override
      public void onClearLines(int c, int s, int st, int hs) {
        listener.onClearLines(c, s, st, hs);
      }
    });
  }

  /**
   * Set an optional listener for game events.
   *
   * <p>Passing {@code null} installs a no-op listener.</p>
   *
   * @param listener callback sink or {@code null} for none
   */
  public void setListener(Listener listener) {
    this.listener = (listener == null) ? new Listener() {
    } : listener;
  }

  /**
   * Attaches an {@link Ai} to steer the game each tick (if present).
   *
   * <p>The AI is consulted during {@link #tick(long)} after gravity is applied. If it returns a
   * non-{@code null} {@link GameIntent}, that intent is applied immediately via
   * {@link #apply(GameIntent)}.</p>
   *
   * @param ai the AI to attach (must not be {@code null})
   *
   * @throws NullPointerException if {@code ai} is {@code null}
   */
  public void ai(Ai ai) {
    this.ai = Objects.requireNonNull(ai, "ai is null");
  }

  /**
   * Apply a high-level input intent to the game.
   *
   * <p>If the game is already over, the intent is ignored. Otherwise, the intent is forwarded to
   * {@link GameState#apply(GameIntent)}. When the state changes, the view is marked dirty to
   * trigger a re-render on the next {@link #tick(long)}, and
   * {@link Listener#onIntentAccepted(GameIntent)} is invoked.</p>
   *
   * @param intent the input to process (must not be {@code null})
   *
   * @throws NullPointerException if {@code intent} is {@code null}
   */
  public void apply(GameIntent intent) {
    if (isGameOver()) {
      return;
    }
    if (state.apply(intent)) {
      view.markDirty();
      listener.onIntentAccepted(intent);
    }
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
      while (gravityAccNanos >= clock.fallIntervalNanos() && !isGameOver()) {
        gravityAccNanos -= clock.fallIntervalNanos();
        if (state.apply(GameIntent.SOFT_DROP)) {
          view.markDirty();
        }
      }
      if (ai != null) {
        GameIntent intent = ai.tick(state.toPureGame());
        if (intent != null) {
          apply(intent);
        }
      }
    }
    view.render(state, clock);
  }

  /**
   * Optional event callbacks for UI/telemetry.
   *
   * <p>Methods are invoked synchronously from {@link #apply(GameIntent)} or
   * {@link #tick(long)}.</p>
   */
  public interface Listener {

    /**
     * Called when an input intent results in a state change.
     */
    default void onIntentAccepted(GameIntent intent) {
    }

    /**
     * Called once when the game reaches a terminal state.
     */
    default void onGameOver() {
    }

    /**
     * Called after lines are cleared and scoring is updated.
     *
     * @param cleared       number of lines cleared in this lock
     * @param score         total score after the clear
     * @param streak        ongoing streak after the clear
     * @param highestStreak maximum streak observed so far
     */
    default void onClearLines(int cleared, int score, int streak, int highestStreak) {
    }

    /**
     * Called when the integer level increases.
     */
    default void onLevelUp(int newLevel) {
    }
  }
}
