package com.example.tetris.ai;

import java.util.Arrays;

public class BoardEvaluator {

  public final int[] heights;
  public final int totalHeight;
  public final int holes;
  public final int bumpiness;
  public final int minHeight;
  public final int maxHeight;
  public final int spireSpread;
  public final int wellSpread;

  private BoardEvaluator(int[] heights, int totalHeight, int holes, int bumpiness,
      int minHeight, int maxHeight, int spireSpread, int wellSpread

  ) {
    this.heights = heights;
    this.totalHeight = totalHeight;
    this.holes = holes;
    this.bumpiness = bumpiness;
    this.maxHeight = maxHeight;
    this.minHeight = minHeight;
    this.spireSpread = spireSpread;
    this.wellSpread = wellSpread;
  }

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
      while (y < height && grid[y][x] == 0) {
        y++;
      }
      int h = height - y;
      heights[x] = h;
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
    return new BoardEvaluator(heights, totalHeight, holes, bumpiness, minHeight, maxHeight,
        Math.max(0, maxHeight - med), Math.max(0, med - minHeight));
  }

  public static int getHeight(int[][] grid) {
    return grid.length;
  }

  public static int getWidth(int[][] grid) {
    return (grid.length == 0) ? 0 : grid[0].length;
  }

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

  private static int median(int[] array) {
    int[] copy = Arrays.copyOf(array, array.length);
    Arrays.sort(copy);
    int num = copy.length;
    return (num == 0) ? 0
        : (num % 2 == 1 ? copy[num / 2] : (copy[num / 2 - 1] + copy[num / 2]) / 2);
  }
}
