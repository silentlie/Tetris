package com.example.tetris.core.board;

/**
 * Represents a single immutable cell in the Tetris grid.
 *
 * <p>A cell is defined by its {@code x} (column index) and {@code y} (row index)
 * coordinates. This record is <b>immutable</b>: all operations return a
 * <b>new {@code Cell}</b> instance instead of modifying the existing one.</p>
 *
 * <p>Common directional constants ({@link #UP}, {@link #DOWN},
 * {@link #LEFT}, {@link #RIGHT}) are provided for convenience when working with relative
 * movements.</p>
 *
 * @param x the x-coordinate (column index)
 * @param y the y-coordinate (row index)
 */
public record Cell(int x, int y) {

  /**
   * A constant representing one cell upward (0, -1).
   */
  public static final Cell UP = new Cell(0, -1);

  /**
   * A constant representing one cell downward (0, 1).
   */
  public static final Cell DOWN = new Cell(0, 1);

  /**
   * A constant representing one cell to the left (-1, 0).
   */
  public static final Cell LEFT = new Cell(-1, 0);

  /**
   * A constant representing one cell to the right (1, 0).
   */
  public static final Cell RIGHT = new Cell(1, 0);

  /**
   * Returns a new {@code Cell} by adding the coordinates of this cell with another cell.
   *
   * <p>If {@code other} is {@code null}, this cell is returned unchanged.</p>
   *
   * <p>Example:</p>
   * <pre>
   *   Cell base = new Cell(3, 4);
   *   Cell offset = new Cell(1, -2);
   *   Cell result = base.add(offset); // (4, 2)
   * </pre>
   *
   * @param other the cell whose coordinates will be added, may be {@code null}
   *
   * @return a new {@code Cell} representing the summed coordinates, never {@code null}
   */
  public Cell add(Cell other) {
    if (other == null) {
      return this;
    }
    return new Cell(x + other.x, y + other.y);
  }

  /**
   * Returns a new {@code Cell} by adding an explicit delta to this cell's coordinates.
   *
   * <p>This method is useful when applying raw offset values without creating
   * an intermediate {@link Cell} object.</p>
   *
   * <p>Example:</p>
   * <pre>
   *   Cell base = new Cell(5, 6);
   *   Cell result = base.add(2, -3); // (7, 3)
   * </pre>
   *
   * @param dx the delta to add to the x-coordinate
   * @param dy the delta to add to the y-coordinate
   *
   * @return a new {@code Cell} representing the updated coordinates, never {@code null}
   */
  public Cell add(int dx, int dy) {
    return new Cell(x + dx, y + dy);
  }
}
