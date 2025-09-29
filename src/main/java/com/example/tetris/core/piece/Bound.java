package com.example.tetris.core.piece;

/**
 * Immutable axis-aligned bounding box (AABB) in integer grid coordinates.
 *
 * <p>A {@code Bound} is the smallest rectangle that encloses a set of points on a 2D integer
 * lattice, defined by inclusive minimum and maximum coordinates on X and Y: {@code [minX, maxX]} ×
 * {@code [minY, maxY]}.</p>
 *
 * <p>Coordinate semantics:</p>
 * <ul>
 *   <li>X increases to the right; Y increases downward (unless your app defines otherwise).</li>
 *   <li>Bounds are inclusive: a point {@code (x, y)} is inside iff
 *       {@code minX <= x <= maxX} and {@code minY <= y <= maxY}.</li>
 *   <li>{@link #width()} and {@link #height()} include both endpoints:
 *       {@code width = maxX - minX + 1}, {@code height = maxY - minY + 1}.</li>
 * </ul>
 *
 * <p>Immutability: this is a Java record; operations such as {@link #translate(int, int)}
 * return new instances.</p>
 *
 * <p>Invariants (not enforced here):</p>
 * <ul>
 *   <li>{@code maxX >= minX}</li>
 *   <li>{@code maxY >= minY}</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * Bound b = new Bound(2, 5, 4, 8);   // x in [2..4], y in [5..8]
 * int w = b.width();                  // 3
 * int h = b.height();                 // 4
 * Bound moved = b.translate(-1, 2);   // x in [1..3], y in [7..10]
 * }</pre>
 *
 * <p>Overflow: near {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE}, calculations in
 * {@link #width()}, {@link #height()}, and {@link #translate(int, int)} can overflow.</p>
 *
 * @param minX the inclusive minimum X coordinate (left edge)
 * @param minY the inclusive minimum Y coordinate (top edge)
 * @param maxX the inclusive maximum X coordinate (right edge); should be {@code >= minX}
 * @param maxY the inclusive maximum Y coordinate (bottom edge); should be {@code >= minY}
 *
 * @apiNote Useful for bounding boxes of shapes (e.g., tetrominoes) or grid regions.
 * @see #width()
 * @see #height()
 * @see #translate(int, int)
 * @since 1.0
 */
public record Bound(int minX, int minY, int maxX, int maxY) {

  /**
   * Returns the number of integer X positions covered by this bound (inclusive).
   *
   * <p>Formula: {@code maxX - minX + 1}.</p>
   *
   * @return the inclusive width in grid units
   */
  public int width() {
    return maxX - minX + 1;
  }

  /**
   * Returns the number of integer Y positions covered by this bound (inclusive).
   *
   * <p>Formula: {@code maxY - minY + 1}.</p>
   *
   * @return the inclusive height in grid units
   */
  public int height() {
    return maxY - minY + 1;
  }

  /**
   * Returns a new bound translated by {@code dx} horizontally and {@code dy} vertically.
   *
   * <p>Equivalent to adding {@code dx} to both {@code minX} and {@code maxX}, and adding
   * {@code dy} to both {@code minY} and {@code maxY}.</p>
   *
   * <p>Example:</p>
   * <pre>{@code
   * new Bound(2, 5, 4, 8).translate(-1, 2); // -> (1, 7) to (3, 10)
   * }</pre>
   *
   * @param dx horizontal delta (positive moves right; negative moves left)
   * @param dy vertical delta (positive moves down; negative moves up)
   *
   * @return a new {@code Bound} shifted by the specified deltas
   */
  public Bound translate(int dx, int dy) {
    return new Bound(minX + dx, minY + dy, maxX + dx, maxY + dy);
  }
}

