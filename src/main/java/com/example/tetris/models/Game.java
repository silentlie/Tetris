package com.example.tetris.models;

import java.util.Arrays;

/**
 * Represents the state of a Tetris game.
 *
 * <p>This class manages the game board, active tetromino, score, and game logic
 * such as movement, rotation, line clearing, and game over detection.</p>
 */
public class Game {

  public static final int K = 10;
  public static final int WIDTH = 10;
  public static final int HEIGHT = 20;

  private final String playerName;
  private int score = 0;

  private final TetrominoType[][] board = new TetrominoType[HEIGHT][WIDTH];
  private Tetromino tetromino;

  /**
   * Creates a new game instance for the given player.
   *
   * @param playerName the player's name
   */
  public Game(String playerName) {
    this.playerName = playerName;
    tetromino = newTetromino();
  }

  /**
   * Spawns a new tetromino at the starting position.
   *
   * @return a newly spawned tetromino
   */
  private Tetromino newTetromino() {
    return new Tetromino(new Cell(WIDTH / 2 - 1, -2));
  }

  /**
   * Checks whether the given tetromino is in a valid position on the board.
   *
   * <p>Blocks above the top row are allowed. Returns {@code false} if the
   * tetromino is out of bounds or collides with locked cells.</p>
   *
   * @param t the tetromino to test
   * @return {@code true} if the tetromino's position is valid; {@code false} otherwise
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
   * Rotates the active tetromino clockwise if possible.
   */
  public void rotate() {
    Tetromino newTetromino = tetromino.rotateClockwise();
    if (checkPosition(newTetromino)) {
      tetromino = newTetromino;
    }
  }

  /**
   * Moves the active tetromino left if possible.
   */
  public void moveLeft() {
    Tetromino newTetromino = tetromino.moveLeft();
    if (checkPosition(newTetromino)) {
      tetromino = newTetromino;
    }
  }

  /**
   * Moves the active tetromino right if possible.
   */
  public void moveRight() {
    Tetromino newTetromino = tetromino.moveRight();
    if (checkPosition(newTetromino)) {
      tetromino = newTetromino;
    }
  }

  /**
   * Moves the active tetromino down if possible.
   *
   * @return {@code true} if the tetromino moved down; {@code false} if blocked
   */
  public boolean moveDown() {
    Tetromino newTetromino = tetromino.moveDown();
    if (checkPosition(newTetromino)) {
      tetromino = newTetromino;
      return true;
    }
    return false;
  }

  /**
   * Locks the active tetromino into the board, clears lines, and spawns a new one.
   *
   * @return {@code true} if the game is over after locking, {@code false} otherwise
   */
  public boolean lock() {
    boolean gameOver = false;
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
    tetromino = newTetromino();
    return gameOver || !checkPosition(tetromino);
  }

  /**
   * Clears completed lines and updates the score.
   *
   * <p>Lines are compacted downward, and empty rows are inserted at the top.
   * The score is incremented by the number of lines cleared.</p>
   */
  private void clearLines() {
    int cleared = 0;
    int writeY = HEIGHT - 1;
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
