package com.example.tetris.ai;

import com.example.tetris.core.board.Cell;
import com.example.tetris.core.piece.Tetromino;
import java.util.Arrays;

/**
 * Small, allocation-friendly board utilities for simulation during AI search.
 *
 * <p>All methods treat the playfield as a rectangular matrix {@code grid[row][col]}
 * with {@code 0} meaning empty and non-zero meaning occupied. Coordinates are screen-style: X
 * increases to the right; Y increases downward. Cells with {@code y < 0} (above the visible top)
 * are allowed in collision checks so a newly spawned piece may straddle the top boundary until it
 * descends on-screen.</p>
 *
 * <p>Typical usage in a move generator:</p>
 * <pre>{@code
 * int[][] afterDrop = BoardSimulator.hardDrop(grid, piece);
 * int[][] afterClear = BoardSimulator.clearLines(afterDrop);
 * BoardEvaluator eval = BoardEvaluator.of(afterClear);
 * }</pre>
 */
public class BoardSimulator {

  /**
   * Returns a deep copy of a 2D int matrix. Rows are copied with {@link Arrays#copyOf(int[], int)};
   * the outer array is newly allocated.
   *
   * @param original source matrix (non-null)
   *
   * @return deep copy with the same dimensions and values
   */
  public static int[][] deepCopy(int[][] original) {
    int[][] copy = new int[original.length][];
    for (int i = 0; i < original.length; i++) {
      copy[i] = Arrays.copyOf(original[i], original[i].length);
    }
    return copy;
  }

  /**
   * Removes all full rows and compacts the remaining rows downward (toward larger Y), filling
   * vacated rows at the top with zeros.
   *
   * <p>Row order of the <em>surviving</em> rows is preserved: the bottom-most
   * non-full row ends up at the bottom of the result, and so on.</p>
   *
   * @param original source grid
   *
   * @return a new grid with all-full rows removed and empty rows inserted on top
   */
  public static int[][] clearLines(int[][] original) {
    int[][] copy = new int[original.length][];
    int write = original.length - 1;

    for (int y = original.length - 1; y >= 0; y--) {
      int[] row = original[y];
      boolean full = true;
      for (int cell : row) {
        if (cell == 0) {
          full = false;
          break;
        }
      }
      if (!full) {
        copy[write--] = Arrays.copyOf(row, row.length);
      }
    }

    // Fill remaining rows with empty rows (all zeros)
    int cols = original.length > 0 ? original[0].length : 0;
    while (write >= 0) {
      copy[write--] = new int[cols];
    }
    return copy;
  }

  /**
   * Simulates a hard drop of {@code tetromino} on {@code original} and returns a new grid with the
   * piece locked in place. If the piece does not currently fit (e.g., spawn collision), the board
   * is returned unchanged (deep-copied).
   *
   * <p>Collision rules: a cell is blocked if it lies outside the horizontal
   * bounds, at or below the bottom of the grid, or overlaps a non-zero cell. Cells with
   * {@code y < 0} are ignored for overlap checks, allowing off-screen spawn area.</p>
   *
   * @param original  source grid
   * @param tetromino active piece to drop; may be partially above the top
   *
   * @return new grid with the dropped piece placed
   */
  public static int[][] hardDrop(int[][] original, Tetromino tetromino) {
    if (tetromino == null || !fits(original, tetromino)) {
      return deepCopy(original);
    }
    for (Tetromino next = tetromino.moveDown(); fits(original, next); next = next.moveDown()) {
      tetromino = next;
    }
    return placePiece(original, tetromino);
  }

  /**
   * Checks whether all blocks of {@code tetromino} lie within horizontal bounds, are strictly above
   * the bottom, and do not overlap existing filled cells (overlap is ignored for cells with
   * {@code y < 0}).
   *
   * @param original  grid to test against
   * @param tetromino piece in absolute coordinates
   *
   * @return {@code true} if the piece can exist in this position
   */
  public static boolean fits(int[][] original, Tetromino tetromino) {
    int height = BoardEvaluator.getHeight(original);
    int width = BoardEvaluator.getWidth(original);
    for (Cell cell : tetromino.getAbsoluteShape()) {
      int x = cell.x();
      int y = cell.y();
      if (x < 0 || x >= width || y >= height || (y >= 0 && original[y][x] != 0)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Writes {@code tetromino} blocks into a copy of {@code original} using value {@code 1} for each
   * occupied cell and returns the new grid. Cells above the visible top ({@code y < 0}) are
   * ignored.
   *
   * <p>Note: if your engine stores piece identifiers or colors in the grid, you
   * may want to replace the constant {@code 1} with a value derived from the piece type.</p>
   */
  private static int[][] placePiece(int[][] original, Tetromino tetromino) {
    int[][] copy = deepCopy(original);
    for (Cell cell : tetromino.getAbsoluteShape()) {
      if (cell.y() >= 0) {
        copy[cell.y()][cell.x()] = 1;
      }
    }
    return copy;
  }
}
