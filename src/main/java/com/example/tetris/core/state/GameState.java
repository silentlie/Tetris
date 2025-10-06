package com.example.tetris.core.state;

import com.example.tetris.core.board.Board;
import com.example.tetris.core.board.Cell;
import com.example.tetris.core.piece.Rotation;
import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.piece.TetrominoType;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Mutable game state for a single Tetris session.
 *
 * <p>Owns and updates the playfield ({@link Board}), the active {@link Tetromino}, the
 * piece {@link PieceQueue}, and simple scoring/streak counters. State transitions are driven by
 * {@link GameIntent} inputs via {@link #apply(GameIntent)}.</p>
 *
 * <p>Coordinate system follows screen-style axes: X increases to the right, Y increases
 * downward. Cells with {@code y < 0} are above the visible top and are permitted during spawn and
 * collision checks.</p>
 *
 * <p>This class is not thread-safe.</p>
 */
public class GameState {

  private final Board board;
  private final PieceQueue queue;

  private Tetromino tetromino;
  private int score = 0;
  private int streak = 0;
  private int highestStreak = 0;
  private boolean gameOver = false;

  private Listener listener = new Listener() {
  };

  /**
   * Create a new game state, spawn the first tetromino from the queue, and check for lock-out.
   *
   * <p>If the freshly spawned tetromino does not fit on the board, the game immediately enters
   * a game-over state.</p>
   *
   * @param board the playfield (must not be {@code null})
   * @param queue the upcoming-piece queue (must not be {@code null})
   *
   * @throws NullPointerException if any argument is {@code null}
   */
  public GameState(Board board, PieceQueue queue) {
    this.board = Objects.requireNonNull(board, "board is null");
    this.queue = Objects.requireNonNull(queue, "queue is null");
    tetromino = spawnFromQueue();
    if (!board.fits(tetromino)) {
      setGameOver();
    }
  }

  private void setGameOver() {
    gameOver = true;
    listener.onGameOver();
  }

  /**
   * Set a listener for game events (optional).
   *
   * <p>Passing {@code null} installs a no-op listener.</p>
   *
   * @param listener callback sink or {@code null} for none
   */
  public void setListener(Listener listener) {
    this.listener = (listener == null) ? new Listener() {
    } : listener;
    if (gameOver) {
      assert listener != null;
      listener.onGameOver();
    }
  }

  /**
   * Apply a high-level input intent to the game state.
   *
   * <p>Semantics:</p>
   * <ul>
   *   <li>{@link GameIntent#ROTATE_CW}, {@link GameIntent#ROTATE_CCW},
   *       {@link GameIntent#MOVE_LEFT}, {@link GameIntent#MOVE_RIGHT} attempt the action once
   *       and succeed only if the resulting position fits.</li>
   *   <li>{@link GameIntent#SOFT_DROP} attempts to move down once; if blocked, the piece locks.
   *   </li>
   *   <li>{@link GameIntent#HARD_DROP} repeatedly moves down until blocked, then locks.</li>
   * </ul>
   *
   * @param intent the input to process (must not be {@code null})
   *
   * @return {@code true} if processed (including when it triggers a lock); {@code false} if the
   *     game is already over and the intent is ignored
   *
   * @throws NullPointerException if {@code intent} is {@code null}
   */
  public boolean apply(GameIntent intent) {
    if (gameOver) {
      return false;
    }
    if (intent == null) {
      return false;
    }
    if (intent != GameIntent.GRAVITY_DROP) {
      listener.onIntent(intent);
    }
    return switch (intent) {
      case ROTATE_CW -> tryApply(Tetromino::rotateClockwise);
      case ROTATE_CCW -> tryApply(Tetromino::rotateCounterClockwise);
      case MOVE_LEFT -> tryApply(Tetromino::moveLeft);
      case MOVE_RIGHT -> tryApply(Tetromino::moveRight);
      case SOFT_DROP -> {
        if (!tryApply(Tetromino::moveDown)) {
          lock();
        }
        yield true;
      }
      case HARD_DROP -> {
        while (true) {
          if (!tryApply(Tetromino::moveDown)) {
            break;
          }
        }
        lock();
        listener.onIntent(intent);
        yield true;
      }
      case GRAVITY_DROP -> {
        if (!tryApply(Tetromino::moveDown)) {
          lock();
        }
        yield false;
      }
    };
  }

  /**
   * Attempts to apply a movement/rotation to the active tetromino if it fits.
   *
   * @param op operation producing a candidate tetromino (must not be {@code null})
   *
   * @return {@code true} if applied (active tetromino updated); {@code false} if blocked
   *
   * @throws NullPointerException if {@code op} is {@code null}
   */
  private boolean tryApply(UnaryOperator<Tetromino> op) {
    Tetromino candidate = op.apply(tetromino);
    if (board.fits(candidate)) {
      tetromino = candidate;
      return true;
    }
    return false;
  }

  /**
   * Lock the active tetromino into the board, clear any full lines, update score/streak, and spawn
   * the next tetromino. If the new piece does not fit, mark game over.
   *
   * <p><strong>Lock-out:</strong> if any locked cell lies above the top ({@code y < 0}),
   * the game ends immediately.</p>
   *
   * <p>Listeners are notified via {@link Listener#onClearLines(int, int, int, int)} after scoring,
   * and via {@link Listener#onGameOver()} if the spawn fails.</p>
   */
  private void lock() {
    boolean lockOut = board.lock(tetromino);
    if (lockOut) {
      setGameOver();
      return;
    }
    int cleared = board.clearLines();
    if (cleared == 0) {
      streak = 0;
    } else {
      streak += cleared;
      highestStreak = Math.max(highestStreak, streak);
      score += calculateScoring(cleared, streak);
      listener.onClearLines(cleared, score, streak, highestStreak);
    }
    tetromino = spawnFromQueue();
    if (!board.fits(tetromino)) {
      setGameOver();
    }
  }

  /**
   * Scoring rule: award {@code streak × 100} for each line cleared in this lock event.
   *
   * <p>Example: if {@code cleared == 2} and the current {@code streak == 3},
   * score gain = {@code 2 × (3 × 100)} = {@code 600}.</p>
   *
   * @param cleared number of lines cleared in this lock (≥ 0)
   * @param streak  cumulative streak after this clear (≥ 0)
   *
   * @return points to add for this lock
   */
  private int calculateScoring(int cleared, int streak) {
    int scoring = 0;
    while (cleared-- > 0) {
      scoring += streak * 100;
    }
    return scoring;
  }

  /**
   * Spawn the next tetromino from the queue at the standard spawn position and orientation.
   *
   * <p>Spawn position is horizontally centered at {@code (board.width / 2 - 1, -2)} with
   * {@link Rotation#UP}.</p>
   *
   * @return the newly spawned tetromino
   */
  private Tetromino spawnFromQueue() {
    TetrominoType type = queue.popAndRefill();
    Cell position = new Cell(board.width / 2 - 1, -2);
    return new Tetromino(type, position, Rotation.UP);
  }

  // ===== Read-only accessors for rendering/HUD =====

  /**
   * Current score (≥ 0).
   */
  public int getScore() {
    return score;
  }

  /**
   * Current ongoing line-clear streak (reset to 0 when a lock clears no lines).
   *
   * @return streak (≥ 0)
   */
  public int getStreak() {
    return streak;
  }

  /**
   * Highest streak achieved so far.
   *
   * @return maximum streak seen (≥ 0)
   */
  public int getHighestStreak() {
    return highestStreak;
  }

  public int getWidth() {
    return board.width;
  }

  public int getHeight() {
    return board.height;
  }

  /**
   * Whether the game has ended (lock-out or post-spawn collision).
   *
   * @return {@code true} if no further inputs are accepted; otherwise {@code false}
   */
  public boolean isGameOver() {
    return gameOver;
  }

  /**
   * Returns the occupying type at a coordinate, or {@code null} if empty/out of bounds.
   *
   * @param x column index
   * @param y row index
   *
   * @return occupying {@link TetrominoType} or {@code null}
   */
  public TetrominoType getCell(int x, int y) {
    return board.getCell(x, y);
  }

  /**
   * Returns the currently active tetromino.
   *
   * @return active {@link Tetromino}
   */
  public Tetromino getTetromino() {
    return tetromino;
  }

  /**
   * Snapshot of the preview queue (head to tail).
   *
   * @return immutable list of upcoming types
   */
  public List<TetrominoType> getQueue() {
    return queue.peek();
  }

  /**
   * Produces a minimal, immutable snapshot of the current game suitable for AI evaluation,
   * serialization, and deterministic tests.
   *
   * <p>The board raster is emitted as a 0/1 matrix ({@code 0} = empty, {@code 1} = occupied),
   * discarding color/type information. The active {@link Tetromino} is passed through, and the
   * "next" field contains the head of the preview queue.</p>
   *
   * @return a {@link PureGame} snapshot of this state
   *
   * @implNote Rows are emitted in screen order (top to bottom). If you later encode piece IDs
   *     or colors, adapt the cell mapping here.
   */
  public PureGame toPureGame() {
    int[][] cells = new int[board.height][board.width];
    for (int y = 0; y < board.height; y++) {
      for (int x = 0; x < board.width; x++) {
        cells[y][x] = (getCell(x, y) == null) ? 0 : 1;
      }
    }
    return new PureGame(board.width, board.height, cells, tetromino, getQueue().getFirst());
  }

  /**
   * Optional event callbacks for UI/telemetry.
   *
   * <p>All methods are invoked synchronously from the caller of {@link #apply(GameIntent)} and
   * {@link #lock()}.</p>
   */
  public interface Listener {

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
     * @param streak        current ongoing streak after the clear
     * @param highestStreak maximum streak observed so far
     */
    default void onClearLines(int cleared, int score, int streak, int highestStreak) {
    }

    /**
     * Called when an input intent results in a state change.
     *
     * @param intent the accepted intent (never {@code null})
     */
    default void onIntent(GameIntent intent) {
    }
  }
}
