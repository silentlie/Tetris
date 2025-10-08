package com.example.tetris.core.piece;

/**
 * Immutable axis-aligned bounding box (AABB) in integer grid coordinates.
 *
 * <p>A {@code Bound} is the smallest rectangle that encloses a set of points on a 2D integer
 * lattice, defined by inclusive minimum and maximum coordinates on X and Y: {@code [minX, maxX]} ×
 * {@code [minY, maxY]}.</p>
 *
 * <h2>Coordinate semantics</h2>
 * <ul>
 *   <li>X increases to the right; Y increases downward (unless your app defines otherwise).</li>
 *   <li>Bounds are <b>inclusive</b>: a point {@code (x, y)} is inside iff
 *       {@code minX <= x <= maxX} and {@code minY <= y <= maxY}.</li>
 *   <li>{@link #width()} and {@link #height()} include both endpoints:
 *       {@code width = maxX - minX + 1}, {@code height = maxY - minY + 1}.</li>
 * </ul>
 *
 * <h2>Immutability</h2>
 * This is a Java {@code record}; operations such as {@link #translate(int, int)} return new
 * instances and never mutate existing ones.
 *
 * <h2>Validation</h2>
 * The canonical constructor enforces {@code maxX >= minX} and {@code maxY >= minY}.
 * Attempts to create an invalid bound throw {@link IllegalArgumentException}.
 *
 * <h2>Overflow</h2>
 * Methods that perform integer arithmetic ({@link #width()}, {@link #height()},
 * {@link #translate(int, int)}) use {@link Math#addExact(int, int)} and
 * {@link Math#subtractExact(int, int)} and will throw {@link ArithmeticException} on overflow.
 *
 * @param minX the inclusive minimum X coordinate (left edge)
 * @param minY the inclusive minimum Y coordinate (top edge)
 * @param maxX the inclusive maximum X coordinate (right edge); must be {@code >= minX}
 * @param maxY the inclusive maximum Y coordinate (bottom edge); must be {@code >= minY}
 */
public record Bound(int minX, int minY, int maxX, int maxY) {

  /**
   * Creates a new {@code Bound}.
   *
   * <p>Enforces {@code maxX >= minX} and {@code maxY >= minY}.</p>
   *
   * @throws IllegalArgumentException if {@code maxX < minX} or {@code maxY < minY}
   */
  public Bound {
    if (maxX < minX) {
      throw new IllegalArgumentException("maxX (" + maxX + ") must be >= minX (" + minX + ")");
    }
    if (maxY < minY) {
      throw new IllegalArgumentException("maxY (" + maxY + ") must be >= minY (" + minY + ")");
    }
  }

  /**
   * Returns the number of integer X positions covered by this bound (inclusive).
   *
   * <p>Formula: {@code maxX - minX + 1}.</p>
   *
   * @return the inclusive width in grid units
   *
   * @throws ArithmeticException if the computation overflows 32-bit signed int
   */
  public int width() {
    return Math.addExact(Math.subtractExact(maxX, minX), 1);
  }

  /**
   * Returns the number of integer Y positions covered by this bound (inclusive).
   *
   * <p>Formula: {@code maxY - minY + 1}.</p>
   *
   * @return the inclusive height in grid units
   *
   * @throws ArithmeticException if the computation overflows 32-bit signed int
   */
  public int height() {
    return Math.addExact(Math.subtractExact(maxY, minY), 1);
  }

  /**
   * Returns a new bound translated by {@code dx} horizontally and {@code dy} vertically.
   *
   * <p>Equivalent to adding {@code dx} to both {@code minX} and {@code maxX}, and adding
   * {@code dy} to both {@code minY} and {@code maxY}.</p>
   *
   * @param dx horizontal delta (positive moves right; negative moves left)
   * @param dy vertical delta (positive moves down; negative moves up)
   *
   * @return a new {@code Bound} shifted by the specified deltas
   *
   * @throws ArithmeticException if any coordinate addition overflows 32-bit signed int
   */
  public Bound translate(int dx, int dy) {
    int newMinX = Math.addExact(minX, dx);
    int newMaxX = Math.addExact(maxX, dx);
    int newMinY = Math.addExact(minY, dy);
    int newMaxY = Math.addExact(maxY, dy);
    // Invariants still hold because we preserve ordering by adding the same delta to each axis.
    return new Bound(newMinX, newMinY, newMaxX, newMaxY);
  }
}
