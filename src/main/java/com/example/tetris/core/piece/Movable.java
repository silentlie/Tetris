package com.example.tetris.core.piece;

import com.example.tetris.core.board.Cell;

/**
 * Represents an immutable contract for entities that can move within a two-dimensional grid.
 *
 * <p>Implementations of this interface must be <b>immutable</b>:
 * all movement operations return a <b>new instance</b> instead of modifying the existing one.</p>
 *
 * <p>This interface uses a self-referential generic type {@code <T>} to enable
 * <i>fluent method chaining</i> for implementing classes or records.</p>
 *
 * @param <T> the concrete implementing type, enabling fluent API returns
 */
public interface Movable<T extends Movable<T>> {

  /**
   * Returns the current absolute position of this entity in the grid.
   *
   * <p>The position usually refers to the "origin" or "reference cell"
   * of the entity, such as the pivot point of a tetromino.</p>
   *
   * @return the current {@link Cell} position, never {@code null}
   */
  Cell position();

  /**
   * Creates a <b>new instance</b> of this entity with the specified position.
   *
   * <p>This method acts as an <b>immutability hook</b>, ensuring the current
   * instance remains unchanged while returning a new one.</p>
   *
   * @param newPosition the new {@link Cell} position, must not be {@code null}
   *
   * @return a new instance of {@code T} with the updated position, never {@code null}
   */
  T withPosition(Cell newPosition);

  /**
   * Moves this entity by a relative offset defined by the given delta.
   *
   * <p>The delta is applied by adding its coordinates to the current position.</p>
   *
   * <p>Example:</p>
   * <pre>
   *   Cell delta = new Cell(2, -1);
   *   T moved = entity.moveBy(delta);
   * </pre>
   *
   * @param delta the relative movement as a {@link Cell} offset, must not be {@code null}
   *
   * @return a new instance of {@code T} at the updated position, never {@code null}
   */
  default T moveBy(Cell delta) {
    return withPosition(position().add(delta));
  }

  /**
   * Moves this entity one cell upward in the grid.
   *
   * <p>This decreases the y-coordinate by 1.</p>
   *
   * @return a new instance of {@code T} one cell up, never {@code null}
   */
  default T moveUp() {
    return moveBy(Cell.UP);
  }

  /**
   * Moves this entity one cell downward in the grid.
   *
   * <p>This increases the y-coordinate by 1.</p>
   *
   * @return a new instance of {@code T} one cell down, never {@code null}
   */
  default T moveDown() {
    return moveBy(Cell.DOWN);
  }

  /**
   * Moves this entity one cell to the left in the grid.
   *
   * <p>This decreases the x-coordinate by 1.</p>
   *
   * @return a new instance of {@code T} one cell left, never {@code null}
   */
  default T moveLeft() {
    return moveBy(Cell.LEFT);
  }

  /**
   * Moves this entity one cell to the right in the grid.
   *
   * <p>This increases the x-coordinate by 1.</p>
   *
   * @return a new instance of {@code T} one cell right, never {@code null}
   */
  default T moveRight() {
    return moveBy(Cell.RIGHT);
  }
}
