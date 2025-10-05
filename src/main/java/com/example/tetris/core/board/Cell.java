package com.example.tetris.core.board;

/**
 * Immutable cell on a 2D integer grid used by the Tetris board.
 *
 * <p>A cell is identified by its {@code x} (column) and {@code y} (row) coordinates.
 * The conventional screen/grid orientation is assumed: X increases to the right, Y increases
 * downward.</p>
 *
 * <p>This is a Java record and therefore shallowly immutable. All operations return
 * new {@code Cell} instances rather than mutating existing ones.</p>
 *
 * <p>Convenience directions ({@link #UP}, {@link #DOWN}, {@link #LEFT}, {@link #RIGHT})
 * are provided for relative movement.</p>
 *
 * @param x the x-coordinate (column index)
 * @param y the y-coordinate (row index)
 */
public record Cell(int x, int y) {

  /**
   * One cell upward: {@code (0, -1)}.
   */
  public static final Cell UP = new Cell(0, -1);

  /**
   * One cell downward: {@code (0, 1)}.
   */
  public static final Cell DOWN = new Cell(0, 1);

  /**
   * One cell to the left: {@code (-1, 0)}.
   */
  public static final Cell LEFT = new Cell(-1, 0);

  /**
   * One cell to the right: {@code (1, 0)}.
   */
  public static final Cell RIGHT = new Cell(1, 0);

  /**
   * Returns a new {@code Cell} whose coordinates are the component-wise sum of this cell and
   * {@code other}.
   *
   * <p>If {@code other} is {@code null}, this cell is returned unchanged.</p>
   *
   * <p>Example:</p>
   * <pre>{@code
   * Cell base = new Cell(3, 4);
   * Cell offset = new Cell(1, -2);
   * Cell result = base.add(offset); // (4, 2)
   * }</pre>
   *
   * @param other the cell to add (maybe {@code null})
   *
   * @return a new {@code Cell} representing the summed coordinates (never {@code null})
   */
  public Cell add(Cell other) {
    if (other == null) {
      return this;
    }
    return add(other.x, other.y);
  }

  /**
   * Returns a new {@code Cell} by adding the provided deltas to this cell’s coordinates.
   *
   * <p>Useful when applying raw offsets without allocating an intermediate {@link Cell}.</p>
   *
   * <p>Example:</p>
   * <pre>{@code
   * Cell base = new Cell(5, 6);
   * Cell result = base.add(2, -3); // (7, 3)
   * }</pre>
   *
   * @param dx the delta applied to {@code x}
   * @param dy the delta applied to {@code y}
   *
   * @return a new {@code Cell} with updated coordinates (never {@code null})
   */
  public Cell add(int dx, int dy) {
    return new Cell(Math.addExact(x, dx), Math.addExact(y, dy));
  }
}
