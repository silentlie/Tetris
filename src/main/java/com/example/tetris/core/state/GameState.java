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
 * upcoming-piece {@link PieceQueue}, and simple scoring/streak counters. State transitions are
 * driven by high-level {@link GameIntent} inputs via {@link #apply(GameIntent)}.</p>
 *
 * <h2>Coordinates</h2>
 *
 * <p>Screen-style axes: X increases rightward; Y increases downward. Cells with {@code y < 0} are
 * above the visible top and are permitted during spawn and collision checks.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Not thread-safe. Call from the JavaFX application thread only.</p>
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
      gameOver = true;
    }
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
        yield true;
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
      gameOver = true;
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
      gameOver = true;
      listener.onGameOver();
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
   */
  public int getStreak() {
    return streak;
  }

  /**
   * Highest streak achieved so far (≥ 0).
   */
  public int getHighestStreak() {
    return highestStreak;
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
   * Board width (columns).
   */
  public int getWidth() {
    return board.width;
  }

  /**
   * Board height (rows).
   */
  public int getHeight() {
    return board.height;
  }

  /**
   * Occupying type at a coordinate, or {@code null} if empty or out of bounds.
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
   * Currently active tetromino (never {@code null} until game over).
   */
  public Tetromino getTetromino() {
    return tetromino;
  }

  /**
   * Snapshot of the preview queue (head to tail).
   *
   * @return immutable list of upcoming types (maybe empty)
   */
  public List<TetrominoType> getQueue() {
    return queue.peek();
  }

  /**
   * Produce a compact, render-ready snapshot of board occupancy and previews.
   *
   * <p>Encodes:</p>
   * <ul>
   *   <li>Settled board as a {@code height × width} 0/1 matrix ({@code 1}=filled).</li>
   *   <li>Active tetromino’s shape matrix for its current rotation.</li>
   *   <li>Next piece’s shape matrix using {@link Rotation#UP}.</li>
   * </ul>
   *
   * <p>Intended for lightweight UIs or AIs that do not require full object graphs.</p>
   *
   * @return a {@link PureGame} DTO capturing board cells and current/next shapes
   */
  public PureGame toCells() {
    int[][] cells = new int[getHeight()][getWidth()];
    for (int y = 0; y < getHeight(); y++) {
      for (int x = 0; x < getWidth(); x++) {
        cells[y][x] = (getCell(x, y) == null) ? 0 : 1;
      }
    }
    int[][] currentShape = tetromino.type().relativeDerived(tetromino.rotation()).toShapeMatrix();
    List<TetrominoType> peek = getQueue();
    TetrominoType nextType = peek.getFirst();
    int[][] nextShape = nextType.relativeDerived(Rotation.UP).toShapeMatrix();
    return new PureGame(getWidth(), getHeight(), cells, currentShape, nextShape);
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
  }
}
