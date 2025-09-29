package com.example.tetris.core.board;

import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.piece.TetrominoType;
import java.util.Arrays;

/**
 * Mutable playfield for Tetris, backed by a {@code height × width} grid of {@link TetrominoType}.
 *
 * <p>Coordinates follow screen-style axes: X increases to the right, Y increases downward.</p>
 *
 * <p>Cells above the visible area (negative Y) are allowed for collision checks so newly spawned
 * tetrominoes can enter from off-screen. Such board are treated as empty in
 * {@link #fits(Tetromino)} and cause a lock-out flag in {@link #lock(Tetromino)} if any placed
 * block lies above the top.</p>
 *
 * <p>Two arrays store state:</p>
 * <ul>
 *   <li>{@code board[y][x]} holds the occupying {@link TetrominoType} or {@code null}.</li>
 *   <li>{@code filled[y]} caches the number of filled board on row {@code y} for fast line clears.
 *   </li>
 * </ul>
 *
 * <p>This class is not thread-safe.</p>
 */
public class Board {

  /**
   * Number of columns.
   */
  public final int width;

  /**
   * Number of rows.
   */
  public final int height;

  private final TetrominoType[][] board;
  private final int[] filled;

  /**
   * Creates an empty board with the given dimensions.
   *
   * <p>All board are initialized to {@code null} (empty) and all fill counts to 0.</p>
   *
   * @param width  number of columns (must be {@code > 0})
   * @param height number of rows (must be {@code > 0})
   */
  public Board(int width, int height) {
    this.width = width;
    this.height = height;
    this.board = new TetrominoType[height][width];
    this.filled = new int[height];
  }

  /**
   * Checks whether the given tetromino can occupy its absolute board on this board.
   *
   * <p>Rules:</p>
   * <ul>
   *   <li>X must be within {@code [0, width)}.</li>
   *   <li>Y below the floor ({@code y >= height}) is invalid.</li>
   *   <li>Y above the top ({@code y < 0}) is permitted and treated as empty.</li>
   *   <li>Occupied board on the board cause failure.</li>
   * </ul>
   *
   * @param t the tetromino to test (must not be {@code null})
   *
   * @return {@code true} if all blocks fit; {@code false} otherwise
   */
  public boolean fits(Tetromino t) {
    for (Cell cell : t.getAbsoluteShape()) {
      int x = cell.x();
      int y = cell.y();
      // OOB horizontally or below floor
      if (x < 0 || x >= width || y >= height) {
        return false;
      }
      // allow above top
      if (y < 0) {
        continue;
      }
      // collision
      if (board[y][x] != null) {
        return false;
      }
    }
    return true;
  }

  /**
   * Locks the given tetromino into the board, updating occupied board and row fill counts.
   *
   * <p>Cells with {@code y < 0} (above the top) are ignored in the grid update but set a
   * lock-out flag.</p>
   *
   * @param t the tetromino to place (must not be {@code null})
   *
   * @return {@code true} if any of the tetromino’s board were above the top (lock-out); otherwise
   *     {@code false}
   */
  public boolean lock(Tetromino t) {
    boolean lockOut = false;
    for (Cell c : t.getAbsoluteShape()) {
      int x = c.x();
      int y = c.y();
      if (y < 0) {
        lockOut = true;
        continue;
      }
      board[y][x] = t.type();
      filled[y]++;
    }
    return lockOut;
  }

  /**
   * Clears all full lines and compacts remaining rows downward.
   *
   * <p>Implementation details:</p>
   * <ul>
   *   <li>Scans from bottom to top maintaining a write pointer; full rows are skipped (cleared),
   *       non-full rows are swapped down to {@code writeY}.</li>
   *   <li>Top {@code cleared} rows are then zeroed (set to {@code null}) and their fill counts
   *   reset.</li>
   *   <li>Runs in {@code O(height × width)} in the worst case.</li>
   * </ul>
   *
   * @return the number of lines cleared (0–{@code height})
   */
  public int clearLines() {
    int cleared = 0;
    int writeY = height - 1;
    for (int y = height - 1; y >= 0; y--) {
      if (filled[y] == width) {
        cleared++;
      } else {
        if (y != writeY) {
          TetrominoType[] tmp = board[writeY];
          board[writeY] = board[y];
          board[y] = tmp;
          int f = filled[writeY];
          filled[writeY] = filled[y];
          filled[y] = f;
        }
        writeY--;
      }
    }
    for (int y = 0; y < cleared; y++) {
      Arrays.fill(board[y], null);
      filled[y] = 0;
    }
    return cleared;
  }

  /**
   * Returns the {@link TetrominoType} at the given cell, or {@code null} if out of bounds or
   * empty.
   *
   * @param c the cell coordinate
   *
   * @return the occupying type or {@code null}
   */
  public TetrominoType getCell(Cell c) {
    int x = c.x();
    int y = c.y();
    return getCell(x, y);
  }

  /**
   * Returns the {@link TetrominoType} at the given coordinates, or {@code null} if out of bounds or
   * empty.
   *
   * @param x column index
   * @param y row index
   *
   * @return the occupying type or {@code null}
   */
  public TetrominoType getCell(int x, int y) {
    if (x < 0 || x >= width || y < 0 || y >= height) {
      return null;
    }
    return board[y][x];
  }
}
