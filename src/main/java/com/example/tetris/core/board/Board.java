package com.example.tetris.core.board;

import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.piece.TetrominoType;
import java.util.Arrays;

/**
 * Mutable Tetris playfield backed by a {@code height × width} grid of {@link TetrominoType}.
 *
 * <p><strong>Coordinate system:</strong> screen-style axes where X increases to the right and
 * Y increases downward.</p>
 *
 * <p><strong>Spawn / off-screen cells:</strong> Cells above the visible area (negative Y) are
 * permitted for collision checks so newly spawned tetrominoes can enter from off-screen. Such cells
 * are treated as empty by {@link #fits(Tetromino)}. When locking a piece via
 * {@link #lock(Tetromino)}, any block with {@code y < 0} is ignored in the grid update but triggers
 * a <em>lock-out</em> indication via the method’s return value.</p>
 *
 * <p><strong>Internal representation:</strong></p>
 * <ul>
 *   <li>{@code board[y][x]} holds the occupying {@link TetrominoType}, or {@code null} if empty.
 *   </li>
 *   <li>{@code filled[y]} caches the number of occupied cells in row {@code y} for fast line
 *   clears.</li>
 * </ul>
 *
 * <p>This class is <strong>not</strong> thread-safe.</p>
 */
public class Board {

  /**
   * Number of columns (X dimension), {@code > 0}.
   */
  public final int width;

  /**
   * Number of rows (Y dimension), {@code > 0}.
   */
  public final int height;

  private final TetrominoType[][] board;
  private final int[] filled;

  /**
   * Creates an empty board with the given dimensions.
   *
   * <p>All cells are initialized to {@code null} (empty) and all row counts to {@code 0}.</p>
   *
   * @param width  number of columns; must be {@code > 0}
   * @param height number of rows; must be {@code > 0}
   */
  public Board(int width, int height) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("width and height must be positive");
    }
    this.width = width;
    this.height = height;
    this.board = new TetrominoType[height][width];
    this.filled = new int[height];
  }

  /**
   * Returns whether {@code t} can occupy its absolute positions on this board.
   *
   * <p><strong>Rules:</strong></p>
   * <ul>
   *   <li>Horizontal bounds: {@code 0 <= x < width} must hold for every block.</li>
   *   <li>Below the floor is invalid: any {@code y >= height} fails.</li>
   *   <li>Above the top is allowed: any {@code y < 0} is treated as empty.</li>
   *   <li>Collision with an occupied cell fails.</li>
   * </ul>
   *
   * @param t the tetromino to test (must not be {@code null})
   *
   * @return {@code true} if all blocks fit without collisions; {@code false} otherwise
   */
  public boolean fits(Tetromino t) {
    for (Cell cell : t.getAbsoluteShape()) {
      int x = cell.x();
      int y = cell.y();
      // Out of bounds horizontally or below the floor
      if (x < 0 || x >= width || y >= height) {
        return false;
      }
      // Allow off-screen above the top
      if (y < 0) {
        continue;
      }
      // Collision with an occupied cell
      if (board[y][x] != null) {
        return false;
      }
    }
    return true;
  }

  /**
   * Locks {@code t} into the grid, marking its blocks and updating row fill counts.
   *
   * <p>Blocks with {@code y < 0} (above the top) are ignored for the grid update but set a
   * <em>lock-out</em> flag.</p>
   *
   * @param t the tetromino to place (must not be {@code null})
   *
   * @return {@code true} if any block of {@code t} was above the top (lock-out); {@code false}
   *     otherwise
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
   * Clears all full lines and compacts remaining rows downward in-place.
   *
   * <p><strong>Algorithm:</strong> Scan from bottom to top maintaining a write pointer.
   * Full rows are skipped (cleared), and non-full rows are swapped down to {@code writeY}. After
   * the pass, the top {@code cleared} rows are zeroed and their fill counts reset.</p>
   *
   * <p>Runs in {@code O(height × width)} time and {@code O(1)} extra space.</p>
   *
   * @return number of lines cleared (from {@code 0} up to {@code height})
   */
  public int clearLines() {
    int cleared = 0;
    int writeY = height - 1;
    for (int y = height - 1; y >= 0; y--) {
      if (filled[y] == width) {
        cleared++;
      } else {
        if (y != writeY) {
          // swap row buffers and their counts
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
    // Clear top 'cleared' rows
    for (int y = 0; y < cleared; y++) {
      Arrays.fill(board[y], null);
      filled[y] = 0;
    }
    return cleared;
  }

  /**
   * Returns the {@link TetrominoType} at coordinate {@code c}, or {@code null} if the position is
   * out of bounds or empty.
   *
   * @param c the cell coordinate (X,Y)
   *
   * @return the occupying type, or {@code null} if empty or out of bounds
   */
  public TetrominoType getCell(Cell c) {
    int x = c.x();
    int y = c.y();
    return getCell(x, y);
  }

  /**
   * Returns the {@link TetrominoType} at {@code (x,y)}, or {@code null} if the position is out of
   * bounds or empty.
   *
   * @param x column index (0-based)
   * @param y row index (0-based)
   *
   * @return the occupying type, or {@code null} if empty or out of bounds
   */
  public TetrominoType getCell(int x, int y) {
    if (x < 0 || x >= width || y < 0 || y >= height) {
      return null;
    }
    return board[y][x];
  }
}
