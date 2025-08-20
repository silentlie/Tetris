package com.example.tetris.models;

/**
 * Represents an axis-aligned bounding box (AABB) in the Tetris grid.
 *
 * <p>A {@code Bounds} defines the minimum and maximum {@code x} and {@code y}
 * coordinates that enclose a set of cells. It is useful for collision detection, rendering, and
 * calculating the occupied space of a tetromino.</p>
 *
 * <p>Bounds are immutable: every operation returns a new {@code Bounds} instance
 * instead of modifying the current one.</p>
 *
 * @param minX the smallest x-coordinate contained in the bounds
 * @param minY the smallest y-coordinate contained in the bounds
 * @param maxX the largest x-coordinate contained in the bounds
 * @param maxY the largest y-coordinate contained in the bounds
 */
public record Bounds(int minX, int minY, int maxX, int maxY) {

  /**
   * Returns a new {@code Bounds} translated by the given {@link Cell}.
   *
   * <p>The translation adds the cell's x and y offsets to both the minimum and
   * maximum coordinates of these bounds. This is typically used when shifting a tetromino's
   * bounding box by its current position.</p>
   *
   * @param c the cell whose x and y values are added as an offset
   * @return a new {@code Bounds} shifted by the given cell's coordinates
   */
  public Bounds add(Cell c) {
    return new Bounds(minX + c.x(), minY + c.y(), maxX + c.x(), maxY + c.y());
  }

  /**
   * Computes the smallest {@code Bounds} that completely encloses the given cells.
   *
   * <p>The method iterates through all provided cells to find the minimum and
   * maximum x and y coordinates. These values are then used to construct a new bounding box.</p>
   *
   * @param cells the array of cells to enclose
   * @return a {@code Bounds} instance covering all provided cells
   * @throws IllegalArgumentException if {@code cells} is {@code null} or empty
   */
  public static Bounds from(Cell[] cells) {
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;
    for (Cell c : cells) {
      int x = c.x();
      int y = c.y();
      if (x < minX) {
        minX = x;
      }
      if (x > maxX) {
        maxX = x;
      }
      if (y < minY) {
        minY = y;
      }
      if (y > maxY) {
        maxY = y;
      }
    }
    return new Bounds(minX, minY, maxX, maxY);
  }
}