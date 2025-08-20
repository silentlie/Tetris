package com.example.tetris.models;

/**
 * Represents a single cell in the Tetris grid.
 *
 * <p>A cell is defined by its {@code x} (column) and {@code y} (row) coordinates. This record
 * is immutable: movement methods return new {@code Cell} instances with updated coordinates.</p>
 *
 * @param x the x-coordinate (column index)
 * @param y the y-coordinate (row index)
 */
public record Cell(int x, int y) {

  /**
   * Returns a new {@code Cell} one row below this cell.
   *
   * <p>This increases the {@code y}-coordinate by 1.</p>
   *
   * @return a new {@code Cell} located one row below
   */
  public Cell moveDown() {
    return new Cell(x, y + 1);
  }

  /**
   * Returns a new {@code Cell} one row above this cell.
   *
   * <p>This decreases the {@code y}-coordinate by 1.</p>
   *
   * @return a new {@code Cell} located one row above
   */
  public Cell moveUp() {
    return new Cell(x, y - 1);
  }

  /**
   * Returns a new {@code Cell} one column to the left of this cell.
   *
   * <p>This decreases the {@code x}-coordinate by 1.</p>
   *
   * @return a new {@code Cell} located one column to the left
   */
  public Cell moveLeft() {
    return new Cell(x - 1, y);
  }

  /**
   * Returns a new {@code Cell} one column to the right of this cell.
   *
   * <p>This increases the {@code x}-coordinate by 1.</p>
   *
   * @return a new {@code Cell} located one column to the right
   */
  public Cell moveRight() {
    return new Cell(x + 1, y);
  }

  /**
   * Returns a new {@code Cell} by adding the coordinates of this cell with another cell.
   *
   * <p>Useful for combining a relative offset with an absolute position.</p>
   *
   * <pre>
   *   Cell base = new Cell(5, 10);
   *   Cell offset = new Cell(2, -1);
   *   Cell result = base.add(offset); // (7, 9)
   * </pre>
   *
   * @param other the other cell whose coordinates will be added
   * @return a new {@code Cell} with summed {@code x} and {@code y}
   */
  public Cell add(Cell other) {
    return new Cell(this.x + other.x, this.y + other.y);
  }
}

