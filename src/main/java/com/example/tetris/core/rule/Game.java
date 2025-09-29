package com.example.tetris.core.rule;

import com.example.tetris.ai.AiService;
import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.state.GameIntent;
import com.example.tetris.core.state.GameState;
import com.example.tetris.core.state.OpMove;
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
 * <p><strong>Thread-safety:</strong> not thread-safe. Call all methods from the JavaFX
 * application thread.</p>
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

  /**
   * Optional AI driver. When set via {@link #ai(AiService)}, {@link #tick(long)} will query for an
   * {@link OpMove} and incrementally steer the current tetromino toward that plan.
   */
  private AiService aiService;

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
   *
   * @throws NullPointerException if any argument is {@code null}
   */
  public Game(GameState state, GameView view, GameClock clock) {
    this.state = Objects.requireNonNull(state, "state is null");
    this.view = Objects.requireNonNull(view, "view is null");
    this.clock = Objects.requireNonNull(clock, "clock is null");
  }

  /**
   * Enables AI control for this game.
   *
   * <p>Once set, each {@link #tick(long)} will retrieve an {@link OpMove} from the AI and issue
   * at most one input toward that plan (one clockwise rotation or one horizontal step). This keeps
   * motion smooth and allows human inputs to interleave.</p>
   *
   * @param aiService the AI service to consult for planning (must not be {@code null})
   *
   * @throws NullPointerException if {@code aiService} is {@code null}
   */
  public void ai(AiService aiService) {
    this.aiService = Objects.requireNonNull(aiService, "aiService is null");
  }

  /**
   * Applies a high-level input intent to the game.
   *
   * <p>If the game is already over, the intent is ignored and {@code false} is returned.
   * Otherwise, the intent is forwarded to {@link GameState#apply(GameIntent)}. When the state
   * changes, the view is marked dirty so the next {@link #tick(long)} will re-render.</p>
   *
   * @param intent the input to process
   *
   * @return {@code true} if the state accepted and applied the intent; {@code false} if ignored
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
   * Steers the current tetromino one step toward the desired AI plan.
   *
   * <p>Order of operations:</p>
   * <ol>
   *   <li>Normalize target rotation to [0..3] and compare with the current rotation. If different,
   *       issue a single clockwise rotation ({@link GameIntent#ROTATE_CW}).</li>
   *   <li>Otherwise, compare current X with {@code opX} and move one cell toward it
   *       ({@link GameIntent#MOVE_LEFT} / {@link GameIntent#MOVE_RIGHT}).</li>
   * </ol>
   *
   * <p>Exactly one input is attempted per call. This method does not drop the piece; gravity and
   * drops are handled elsewhere.</p>
   *
   * @param opMove the target column ({@code opX}) and rotation index ({@code opRotate})
   */
  private void apply(OpMove opMove) {
    Tetromino t = state.getTetromino();
    if (t == null) {
      return;
    }

    int want = ((opMove.opRotate() % 4) + 4) % 4;
    int have = t.rotation().ordinal();

    if (want != have) {
      apply(GameIntent.ROTATE_CW);
      return;
    }

    int curX = t.position().x();
    int targetX = opMove.opX();

    if (curX < targetX) {
      apply(GameIntent.MOVE_RIGHT);
      return;
    }
    if (curX > targetX) {
      apply(GameIntent.MOVE_LEFT);
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
   * Advances time, applies gravity steps as needed, optionally steers via AI, and renders the
   * frame.
   *
   * <p>Behavior:</p>
   * <ul>
   *   <li>If the game is not over, advance {@link GameClock} by {@code dtNanos}.</li>
   *   <li>Accumulate {@code dtNanos} into an internal counter. While the accumulator is
   *       ≥ {@link GameClock#fallIntervalNanos()}, subtract one interval and attempt a
   *       {@link GameIntent#SOFT_DROP}. If the state changes, mark the view dirty.</li>
   *   <li>If an {@link AiService} is present, request an {@link OpMove} and apply at most one input
   *       toward the plan (rotate or horizontal move). AI steering does not trigger drops.</li>
   *   <li>Render the current state via {@link GameView}, regardless of game-over status.</li>
   * </ul>
   *
   * <p>Call once per animation frame. Negative deltas are ignored.</p>
   *
   * @param dtNanos elapsed time since the previous frame in nanoseconds (maybe 0)
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
      if (aiService != null) {
        OpMove plan = aiService.tick(state);
        if (plan != null) {
          apply(plan);
        }
      }
    }
    view.render(state, clock);
  }
}