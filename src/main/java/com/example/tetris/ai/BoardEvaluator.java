package com.example.tetris.ai;

import java.util.Arrays;

/**
 * Computes column-wise features for a Tetris board snapshot and exposes commonly used aggregate
 * metrics for AI heuristics.
 *
 * <p>Given a grid {@code int[rows][cols]} where {@code 0} denotes empty and
 * non-zero denotes filled, this evaluator derives per-column heights and summary statistics such as
 * total height, holes, bumpiness, and spread measures. The class is immutable: all fields are
 * {@code final} and populated at construction via {@link #of(int[][])}.</p>
 *
 * <h2>Definitions</h2>
 * <ul>
 *   <li><b>Column height:</b> number of filled cells in a column.</li>
 *   <li><b>Holes:</b> empty cells that appear strictly below the topmost filled
 *       cell in a column.</li>
 *   <li><b>Bumpiness:</b> sum of absolute differences between adjacent column
 *       heights.</li>
 *   <li><b>Spire spread:</b> {@code maxHeight - median(height)}; how much the
 *       tallest column sticks up above the middle of the distribution.</li>
 *   <li><b>Well spread:</b> {@code median(height) - minHeight}; how deep the
 *       lowest column sits below the middle of the distribution.</li>
 * </ul>
 *
 * <p>These quantities are useful for simple linear-evaluation AIs as well as
 * feature inputs for learned policies.</p>
 */
public class BoardEvaluator {

  /**
   * Per-column heights; length equals board width.
   */
  public final int[] heights;
  /**
   * Sum of all column heights.
   */
  public final int totalHeight;
  /**
   * Count of empty cells beneath the first filled cell in each column.
   */
  public final int holes;
  /**
   * Sum of |height[i] - height[i-1]| across columns (left-to-right).
   */
  public final int bumpiness;
  /**
   * Minimum column height across the board.
   */
  public final int minHeight;
  /**
   * Maximum column height across the board.
   */
  public final int maxHeight;
  /**
   * How far the tallest column rises above the median column height.
   */
  public final int spireSpread;
  /**
   * How far the lowest column lies below the median column height.
   */
  public final int wellSpread;

  /**
   * Constructs an immutable instance. Prefer {@link #of(int[][])} which computes all metrics from a
   * grid.
   */
  private BoardEvaluator(
      int[] heights,
      int totalHeight,
      int holes,
      int bumpiness,
      int minHeight,
      int maxHeight,
      int spireSpread,
      int wellSpread) {
    this.heights = heights;
    this.totalHeight = totalHeight;
    this.holes = holes;
    this.bumpiness = bumpiness;
    this.maxHeight = maxHeight;
    this.minHeight = minHeight;
    this.spireSpread = spireSpread;
    this.wellSpread = wellSpread;
  }

  /**
   * Builds a {@code BoardEvaluator} by scanning the given grid.
   *
   * <p>The grid is interpreted as {@code grid[row][col]} with 0 == empty, non-zero == filled.
   * The scan proceeds column-wise to compute heights and holes; bumpiness is the sum of absolute
   * deltas between adjacent column heights. The median of the heights is used to derive
   * {@link #spireSpread} and {@link #wellSpread} as non-negative values.</p>
   *
   * @param grid rectangular matrix of cells; must be non-null. If {@code grid} has zero rows, width
   *             is treated as 0; if it has rows of length 0, width is 0 as well.
   *
   * @return an immutable evaluator capturing per-column and aggregate features
   */
  public static BoardEvaluator of(int[][] grid) {
    int height = getHeight(grid);
    int width = getWidth(grid);

    int[] heights = new int[width];
    int holes = 0;
    int totalHeight = 0;
    int maxHeight = 0;
    int minHeight = Integer.MAX_VALUE;
    int bumpiness = 0;
    int prevHeight = 0;

    for (int x = 0; x < width; x++) {
      int y = 0;
      // Find the first filled cell in the column
      while (y < height && grid[y][x] == 0) {
        y++;
      }
      int h = height - y;
      heights[x] = h;

      // Count holes below the first filled cell
      while (++y < height) {
        if (grid[y][x] == 0) {
          holes++;
        }
      }

      totalHeight += h;
      if (h > maxHeight) {
        maxHeight = h;
      }
      if (h < minHeight) {
        minHeight = h;
      }

      if (x > 0) {
        bumpiness += Math.abs(h - prevHeight);
      }
      prevHeight = h;
    }

    int med = median(heights);
    return new BoardEvaluator(
        heights,
        totalHeight,
        holes,
        bumpiness,
        minHeight == Integer.MAX_VALUE ? 0 : minHeight,
        maxHeight,
        Math.max(0, maxHeight - med),
        Math.max(0, med - minHeight));
  }

  /**
   * Returns the number of rows in {@code grid}.
   */
  public static int getHeight(int[][] grid) {
    return grid.length;
  }

  /**
   * Returns the number of columns in {@code grid} (0 if there are no rows).
   */
  public static int getWidth(int[][] grid) {
    return (grid.length == 0) ? 0 : grid[0].length;
  }

  /**
   * Counts the number of rows whose cells are all non-zero.
   *
   * @param grid the board matrix
   *
   * @return how many fully filled rows are present (useful for scoring clears)
   */
  public static int countFullRows(int[][] grid) {
    int count = 0;
    for (int[] row : grid) {
      boolean full = true;
      for (int v : row) {
        if (v == 0) {
          full = false;
          break;
        }
      }
      if (full) {
        count++;
      }
    }
    return count;
  }

  /**
   * Median of an integer array using copy+sort; returns 0 for an empty array. For even length,
   * returns the integer average of the two middle values.
   */
  private static int median(int[] array) {
    int[] copy = Arrays.copyOf(array, array.length);
    Arrays.sort(copy);
    int num = copy.length;
    return (num == 0) ? 0
        : (num % 2 == 1 ? copy[num / 2] : (copy[num / 2 - 1] + copy[num / 2]) / 2);
  }
}
