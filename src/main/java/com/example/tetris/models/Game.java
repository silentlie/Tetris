package com.example.tetris.models;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;

/**
 * Core game state and rules for a single Tetris session.
 *
 * <p>This class owns and updates the playfield (board), the currently falling
 * {@link Tetromino}, and the player's score. It implements the fundamental mechanics: movement,
 * rotation, gravity (soft drop), locking, line clearing, and game-over detection.</p>
 *
 * <h2>Determinism & RNG</h2>
 *
 * <p>Piece generation uses a {@link java.util.SplittableRandom} seeded via the constructor.
 * Using the same {@code seed} produces the same piece sequence (useful for replays and synchronized
 * PvP). Selection is uniform over {@link TetrominoType#values()} (no 7-bag).</p>
 *
 * <h2>Coordinates</h2>
 * <ul>
 *   <li>Board indices are {@code [row][col]} (i.e., {@code [y][x]}).</li>
 *   <li>Cells with {@code y < 0} (above the visible top) are permitted for spawn/entry.</li>
 *   <li>Out-of-bounds means {@code x < 0}, {@code x >= width}, or {@code y >= height}.</li>
 * </ul>
 *
 * <h2>Next Queue</h2>
 *
 * <p>The game maintains a fixed-size queue of the next three {@link TetrominoType}s.
 * Spawning consumes the head and immediately refills the tail using the same seeded RNG,
 * keeping the queue size constant. Use {@link #peekNextTypes()} for UI previews.</p>
 *
 * <h2>Scoring</h2>
 *
 * <p>Scoring uses a <b>combo streak per cleared line</b> model:</p>
 * <ul>
 *   <li>If a lock clears no lines, the streak resets to 0.</li>
 *   <li>If a lock clears {@code N > 0} lines, the game increments {@code streak} once
 *       <em>per line</em> and adds {@code +100 × streak} each increment.</li>
 * </ul>
 *
 * <p>Examples (starting from {@code streak = 0}):</p>
 * <ul>
 *   <li>Clear 1 line → streak: 1 → +100.</li>
 *   <li>Next lock clears 1 line → streak: 2 → +200.</li>
 *   <li>Next lock clears 2 lines → streak: 3 then 4 → +300 +400 (total +700).</li>
 * </ul>
 *
 * <p>This intentionally makes multi-line clears within a single lock very rewarding.
 * (Hook point: multiply by a level factor in the future.)</p>
 *
 * <h2>Threading</h2>
 *
 * <p>All public methods are intended to be invoked on the JavaFX Application Thread.</p>
 */
public class Game {

  /**
   * Number of upcoming pieces exposed to the UI (peek size).
   */
  private static final int NEXT_QUEUE_SIZE = 3;

  /**
   * Board width in cells.
   */
  public final int width;

  /**
   * Board height in cells (visible rows).
   */
  public final int height;

  private final String playerName;

  /**
   * The playfield grid; {@code null} entries are empty cells.
   *
   * <p>Indexed as {@code [row][col]} (i.e., {@code [y][x]}).</p>
   */
  private final TetrominoType[][] board;

  private int score = 0;

  /**
   * Current combo streak; increments once per line cleared and resets when a lock clears none.
   */
  private int streak = 0;

  /**
   * The currently falling tetromino.
   *
   * <p>Never {@code null} during normal play after construction. It may become
   * effectively invalid when a lock causes game over (e.g., blocks locked above the top or next
   * spawn is illegal).</p>
   */
  private Tetromino tetromino;

  /**
   * FIFO of upcoming piece types (head = next to spawn).
   *
   * <p>Always kept at size {@value #NEXT_QUEUE_SIZE}: spawning removes the head
   * and the tail is refilled from the seeded RNG.</p>
   */
  private final ArrayDeque<TetrominoType> nextQueue = new ArrayDeque<>(NEXT_QUEUE_SIZE);

  /**
   * Seed used to initialize the piece generator for deterministic sequences.
   */
  private final long seed;

  /**
   * Pseudorandom generator for piece selection; seeded for reproducibility.
   */
  private final SplittableRandom rng;

  /**
   * Constructs a new game instance and spawns the first tetromino.
   *
   * <p>Initializes the board, fills the next-queue to exactly three items,
   * then spawns the active piece by consuming the queue head. The queue is immediately refilled to
   * maintain its fixed size.</p>
   *
   * @param playerName the player's display name (used by UI/scoreboard); must not be {@code null}
   * @param seed       the RNG seed controlling piece sequence (store this for replays/PvP)
   * @param width      board width in cells
   * @param height     board height in cells (visible rows)
   */
  public Game(String playerName, long seed, int width, int height) {
    this.playerName = playerName;
    this.seed = seed;
    this.rng = new SplittableRandom(seed);
    this.width = width;
    this.height = height;
    board = new TetrominoType[height][width];
    fillNextQueueToSize();
    tetromino = spawnFromQueue();
  }

  /**
   * Draws a random tetromino type using the seeded RNG (uniform; not 7-bag).
   */
  private TetrominoType randomType() {
    TetrominoType[] values = TetrominoType.values();
    return values[rng.nextInt(values.length)];
  }

  /**
   * Tops up {@link #nextQueue} to {@value #NEXT_QUEUE_SIZE} using {@link #randomType()}.
   */
  private void fillNextQueueToSize() {
    while (nextQueue.size() < NEXT_QUEUE_SIZE) {
      nextQueue.addLast(randomType());
    }
  }

  /**
   * Consumes the next piece from the queue and returns a newly spawned {@link Tetromino}.
   *
   * <p>After removal, the queue is refilled to maintain its fixed size. The piece spawns
   * centered horizontally with {@link Rotation#UP}, and may begin with {@code y < 0} (above the
   * visible top) to allow entry.</p>
   *
   * @return a freshly spawned tetromino determined by the queue head
   */
  private Tetromino spawnFromQueue() {
    TetrominoType type = nextQueue.removeFirst();
    fillNextQueueToSize();
    // Standard spawn position/orientation
    Cell position = new Cell(width / 2 - 1, -2);
    Rotation rotation = Rotation.UP;
    return new Tetromino(type, position, rotation);
  }

  /**
   * Validates that a tetromino's blocks are within bounds and do not collide with locked cells.
   *
   * <p>Rules:</p>
   * <ul>
   *   <li>Cells with {@code y < 0} (above the top) are allowed.</li>
   *   <li>Any cell with {@code x < 0}, {@code x >= WIDTH}, or {@code y >= height} is invalid.</li>
   *   <li>Any cell overlapping a non-{@code null} board cell is invalid (collision).</li>
   * </ul>
   *
   * @param t the tetromino to test; must not be {@code null}
   * @return {@code true} if all blocks are legal against the current board; {@code false} otherwise
   */
  public boolean checkPosition(Tetromino t) {
    return t.getAbsoluteShape().stream().allMatch(cell -> {
      int x = cell.x();
      int y = cell.y();
      // allow above top
      if (y < 0) {
        return true;
      }
      // OOB
      if (x < 0 || x >= width || y >= height) {
        return false;
      }
      // no collision
      return board[y][x] == null;
    });
  }

  /**
   * Attempts to rotate the active tetromino clockwise.
   *
   * <p>No wall-kick logic is applied here. If the rotated position would be illegal (out of
   * bounds or colliding), the rotation is ignored.</p>
   */
  public void rotate() {
    Tetromino newTetromino = tetromino.rotateClockwise();
    if (checkPosition(newTetromino)) {
      tetromino = newTetromino;
    }
  }

  /**
   * Attempts to move the active tetromino one cell to the left.
   *
   * <p>No-op if the target position is illegal.</p>
   */
  public void moveLeft() {
    Tetromino newTetromino = tetromino.moveLeft();
    if (checkPosition(newTetromino)) {
      tetromino = newTetromino;
    }
  }

  /**
   * Attempts to move the active tetromino one cell to the right.
   *
   * <p>No-op if the target position is illegal.</p>
   */
  public void moveRight() {
    Tetromino newTetromino = tetromino.moveRight();
    if (checkPosition(newTetromino)) {
      tetromino = newTetromino;
    }
  }

  /**
   * Advances the active tetromino downward by one row (gravity step) if possible.
   *
   * @return {@code true} if the piece moved; {@code false} if blocked by floor or stack
   */
  public boolean softDrop() {
    Tetromino newTetromino = tetromino.moveDown();
    if (checkPosition(newTetromino)) {
      tetromino = newTetromino;
      return true;
    }
    return false;
  }

  /**
   * Locks the active tetromino into the board, clears completed lines, and spawns the next piece.
   *
   * <p>After locking, {@link #clearLines()} compacts the board and applies scoring/streak.
   * Then a new piece is spawned from the next-queue; if its initial placement is illegal, the game
   * ends.</p>
   *
   * @return {@code true} if the game is over after locking; {@code false} otherwise
   */
  public boolean lock() {
    boolean gameOver = false;
    // Place current piece into the board; detect lock-out if any block is above the top.
    for (Cell cell : tetromino.getAbsoluteShape()) {
      int x = cell.x();
      int y = cell.y();
      if (y < 0) {
        gameOver = true;
        continue;
      }
      board[y][x] = tetromino.type();
    }
    clearLines();
    // Spawn next piece; if it can't legally exist, game is over.
    tetromino = spawnFromQueue();
    return gameOver || !checkPosition(tetromino);
  }

  /**
   * Clears all full lines, compacts remaining rows downward, inserts empty rows at the top, and
   * updates combo streak and score.
   *
   * <p><b>Algorithm:</b></p>
   * <ol>
   *   <li>Sweep bottom-to-top, copying non-full rows downward to close gaps.</li>
   *   <li>Count full rows; then set that many top rows to {@code null} (empty).</li>
   *   <li><b>Scoring:</b>
   *     <ul>
   *       <li>If no lines are cleared, reset {@code streak} to 0.</li>
   *       <li>If {@code cleared &gt; 0}, for each cleared line:
   *         increment {@code streak} and add {@code +100 × streak} to {@code score}.</li>
   *     </ul>
   *   </li>
   * </ol>
   *
   * <p>This method defines the combo policy (per-line streak increments). Adjust here if you add
   * classic baseline values, level multipliers, back-to-back bonuses, T-Spins, or alternative
   * combo semantics.</p>
   */
  private void clearLines() {
    int cleared = 0;
    int writeY = board.length - 1;
    for (int y = board.length - 1; y >= 0; y--) {
      TetrominoType[] row = board[y];
      if (Arrays.stream(row).noneMatch(Objects::isNull)) {
        cleared++;
      } else {
        if (y != writeY) {
          System.arraycopy(row, 0, board[writeY], 0, row.length);
        }
        writeY--;
      }
    }
    for (int y = 0; y < cleared; y++) {
      Arrays.fill(board[y], null);
    }
    if (cleared == 0) {
      streak = 0;
    }
    while (cleared > 0) {
      streak++;
      // May multiply with level int
      score += streak * 100;
      cleared--;
    }
  }

  /**
   * Returns the player's name.
   *
   * @return the player's name, never {@code null}
   */
  public String getPlayerName() {
    return playerName;
  }

  /**
   * Returns the current score.
   *
   * <p>The score accrues via the per-line combo rule:
   * each cleared line increments {@code streak} and awards {@code +100 × streak}. The streak resets
   * to 0 on a lock that clears no lines.</p>
   *
   * @return the player's score (non-negative)
   */
  public int getScore() {
    return score;
  }

  /**
   * Returns a defensive copy of the current board state.
   *
   * <p>The returned 2D array is a deep copy; modifying it will not affect the internal board.</p>
   *
   * @return a deep copy of the board, indexed as {@code [y][x]}
   */
  public TetrominoType[][] getBoard() {
    return Arrays.stream(board)
        .map(row -> Arrays.copyOf(row, width))
        .toArray(TetrominoType[][]::new);
  }

  /**
   * Returns the currently active tetromino.
   *
   * @return the active tetromino, never {@code null} during play
   */
  public Tetromino getTetromino() {
    return tetromino;
  }

  /**
   * Returns the RNG seed used by this game instance.
   *
   * <p>Store this value to reproduce the same piece sequence for replays or synchronized
   * matches (e.g., per-player streams in local PvP).</p>
   *
   * @return the seed that initialized the piece generator
   */
  public long getSeed() {
    return seed;
  }

  /**
   * Returns an immutable snapshot (head-first) of the upcoming piece types for UI rendering. The
   * list size is always {@value #NEXT_QUEUE_SIZE}.
   *
   * @return an unmodifiable list of the next three tetromino types in spawn order
   */
  public List<TetrominoType> peekNextTypes() {
    // Preserve queue order (head to tail) in a stable List
    List<TetrominoType> list = new ArrayList<>(nextQueue);
    return Collections.unmodifiableList(list);
  }
}
