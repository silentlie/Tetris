package com.example.tetris.models;

import java.util.Arrays;

/**
 * Represents the state and rules of a Tetris game.
 *
 * <p>Owns the board, active tetromino, score, and implements core mechanics:
 * movement, rotation, gravity step (soft drop), locking, line clearing, and game-over detection.
 * All methods are intended to be called on the JavaFX Application Thread.</p>
 */
public class Game {

  public static final int K = 10;
  public static final int WIDTH = 10;
  public static final int HEIGHT = 20;

  private final String playerName;
  private int score = 0;

  /**
   * Board cells; {@code null} means empty. Indexed as [row][col] i.e., [y][x].
   */
  private final TetrominoType[][] board = new TetrominoType[HEIGHT][WIDTH];
  /**
   * Currently falling tetromino. Never {@code null} after construction unless locking creates game
   * over.
   */
  private Tetromino tetromino;

  /**
   * Creates a new game instance for the given player and spawns the first tetromino.
   *
   * @param playerName the player's name (used externally for UI/scoreboard)
   */
  public Game(String playerName) {
    this.playerName = playerName;
    tetromino = newTetromino();
  }

  /**
   * Spawns a new tetromino at the standard starting column, slightly above the visible board.
   *
   * <p>Blocks above the top are permitted until they enter the field.</p>
   */
  private Tetromino newTetromino() {
    return Tetromino.randomTetromino(new Cell(WIDTH / 2 - 1, -2));
  }

  /**
   * Validates that a tetromino's blocks are within bounds and not colliding with locked cells.
   *
   * <p>Cells with {@code y < 0} (above the top) are allowed. Any cell with {@code x < 0},
   * {@code x >= WIDTH}, or {@code y >= HEIGHT} is invalid, as is collision with a non-null board
   * cell.</p>
   *
   * @param t tetromino to test
   *
   * @return {@code true} if every block is legal against the board; {@code false} otherwise
   */
  public boolean checkPosition(Tetromino t) {
    for (Cell cell : t.getAbsoluteShape()) {
      int x = cell.x();
      int y = cell.y();
      // allow blocks above the top
      if (y < 0) {
        continue;
      }
      // out of bounds
      if (x < 0 || x >= WIDTH || y >= HEIGHT) {
        return false;
      }
      // collides with locked
      if (board[y][x] != null) {
        return false;
      }
    }
    return true;
  }

  /**
   * Rotates the active tetromino clockwise; no-ops if the rotated position is illegal.
   */
  public void rotate() {
    Tetromino newTetromino = tetromino.rotateClockwise();
    if (checkPosition(newTetromino)) {
      tetromino = newTetromino;
    }
  }

  /**
   * Moves the active tetromino left; no-ops if the target position is illegal.
   */
  public void moveLeft() {
    Tetromino newTetromino = tetromino.moveLeft();
    if (checkPosition(newTetromino)) {
      tetromino = newTetromino;
    }
  }

  /**
   * Moves the active tetromino right; no-ops if the target position is illegal.
   */
  public void moveRight() {
    Tetromino newTetromino = tetromino.moveRight();
    if (checkPosition(newTetromino)) {
      tetromino = newTetromino;
    }
  }

  /**
   * Advances the active tetromino downward by one row if possible.
   *
   * @return {@code true} if the piece moved; {@code false} if blocked by floor/stack
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
   * <p><strong>Game over:</strong> If any block of the locked piece lies above the visible field
   * (i.e., {@code y < 0}), or if the freshly spawned piece cannot be placed legally, the game
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
    tetromino = newTetromino();
    return gameOver || !checkPosition(tetromino);
  }

  /**
   * Clears all full lines, compacts remaining rows downward, inserts empty rows at the top, and
   * increments the score by the number of cleared lines (simple scoring).
   *
   * <p>Scoring: +1 per line. Adjust here if you later add T-Spins/back-to-back/etc.</p>
   */
  private void clearLines() {
    int cleared = 0;
    int writeY = HEIGHT - 1;
    // Sweep from bottom to top, copying non-full rows downward.
    for (int y = HEIGHT - 1; y >= 0; y--) {
      boolean full = true;
      for (TetrominoType cell : board[y]) {
        if (cell == null) {
          full = false;
          break;
        }
      }
      if (full) {
        cleared++;
      } else {
        if (y != writeY) {
          System.arraycopy(board[y], 0, board[writeY], 0, board[y].length);
        }
        writeY--;
      }
    }
    // Insert cleared empty rows at the top.
    for (int i = 0; i < cleared; i++) {
      Arrays.fill(board[i], null);
    }
    score += cleared;
  }

  /**
   * Returns the name of the player.
   *
   * @return the player's name
   */
  public String getPlayerName() {
    return playerName;
  }

  /**
   * Returns the current score.
   *
   * @return the player's score
   */
  public int getScore() {
    return score;
  }

  /**
   * Returns a copy of the current board state.
   *
   * <p>The returned array is a deep copy; modifications will not affect
   * the internal board.</p>
   *
   * @return a copy of the board
   */
  public TetrominoType[][] getBoard() {
    TetrominoType[][] copy = new TetrominoType[HEIGHT][];
    for (int i = 0; i < HEIGHT; i++) {
      copy[i] = Arrays.copyOf(board[i], WIDTH);
    }
    return copy;
  }

  /**
   * Returns the current active tetromino.
   *
   * @return the active tetromino
   */
  public Tetromino getTetromino() {
    return tetromino;
  }
}
