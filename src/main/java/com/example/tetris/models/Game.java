package com.example.tetris.models;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents the state and core rules of a Tetris game.
 *
 * <p>This class owns the playfield (board), the currently falling {@link Tetromino},
 * the player's score, and implements the main mechanics: movement, rotation, gravity (soft drop),
 * locking, line clearing, and game-over detection.</p>
 *
 * <p><b>Threading:</b> All public methods are intended to be invoked on the JavaFX
 * Application Thread.</p>
 */
public class Game {

  public static final int K = 10;

  /**
   * Board width in cells.
   */
  public static final int WIDTH = 10;

  /**
   * Board height in cells (visible rows).
   */
  public static final int HEIGHT = 20;

  private final String playerName;
  private int score = 0;

  /**
   * The playfield grid; {@code null} entries are empty cells.
   *
   * <p>Indexed as {@code [row][col]} (i.e., {@code [y][x]}).</p>
   */
  private final TetrominoType[][] board = new TetrominoType[HEIGHT][WIDTH];

  /**
   * The currently falling tetromino.
   *
   * <p>Never {@code null} after construction unless a lock operation results in game over.</p>
   */
  private Tetromino tetromino;

  /**
   * Creates a new game instance for the given player and spawns the first tetromino.
   *
   * @param playerName the player's name (for UI/scoreboard use)
   */
  public Game(String playerName) {
    this.playerName = playerName;
    tetromino = newTetromino();
  }

  /**
   * Spawns a new tetromino at the standard starting column, slightly above the visible board.
   *
   * <p>Blocks with {@code y < 0} (above the top) are permitted until they enter the field.</p>
   *
   * @return a newly spawned {@link Tetromino}
   */
  private Tetromino newTetromino() {
    return Tetromino.randomTetromino(new Cell(WIDTH / 2 - 1, -2));
  }

  /**
   * Validates that a tetromino's blocks are within bounds and do not collide with already locked
   * cells on the board.
   *
   * <p>Rules:</p>
   * <ul>
   *   <li>Cells with {@code y < 0} (above the top) are allowed.</li>
   *   <li>Any cell with {@code x < 0}, {@code x >= WIDTH}, or {@code y >= HEIGHT} is invalid.</li>
   *   <li>Any cell overlapping a non-{@code null} board cell is invalid (collision).</li>
   * </ul>
   *
   * @param t the tetromino to test, must not be {@code null}
   *
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
      if (x < 0 || x >= WIDTH || y >= HEIGHT) {
        return false;
      }
      // no collision
      return board[y][x] == null;
    });
  }

  /**
   * Attempts to rotate the active tetromino clockwise.
   *
   * <p>If the rotated position would be illegal (out of bounds or colliding),
   * the method performs no change.</p>
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
   * <p><b>Game over:</b> Occurs if any block of the just-locked piece lies above the visible field
   * (i.e., {@code y < 0}) or if the freshly spawned tetromino cannot be placed legally.</p>
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
    tetromino = newTetromino();
    return gameOver || !checkPosition(tetromino);
  }

  /**
   * Clears all full lines, compacts remaining rows downward, inserts empty rows at the top, and
   * increments the score by the number of cleared lines (simple scoring: +1 per line).
   *
   * <p>Algorithm:</p>
   * <ol>
   *   <li>Sweep bottom-to-top, copying non-full rows downward to close gaps.</li>
   *   <li>Count full rows; then set that many top rows to {@code null} (empty).</li>
   *   <li>Increment {@link #score} by the number of cleared lines.</li>
   * </ol>
   *
   * <p>Adjust here if you later add T-Spins, back-to-back, combo bonuses, etc.</p>
   */
  private void clearLines() {
    int cleared = (int) Arrays.stream(board)
        .filter(row -> Arrays.stream(row).allMatch(Objects::nonNull))
        .count();
    TetrominoType[][] remaining = Arrays.stream(board)
        .filter(row -> Arrays.stream(row).anyMatch(Objects::isNull))
        .toArray(TetrominoType[][]::new);
    for (int i = 0; i < cleared; i++) {
      Arrays.fill(board[i], null);
    }
    for (int i = 0; i < remaining.length; i++) {
      System.arraycopy(remaining[i], 0, board[cleared + i], 0, remaining[i].length);
    }
    score += cleared;
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
        .map(row -> Arrays.copyOf(row, WIDTH))
        .toArray(TetrominoType[][]::new);
  }


  /**
   * Returns the currently active tetromino.
   *
   * @return the active {@link Tetromino}, never {@code null} during play
   */
  public Tetromino getTetromino() {
    return tetromino;
  }
}
