package com.example.tetris.core.piece;

import com.example.tetris.core.board.Cell;


/**
 * Immutable contract for entities that can move on a two-dimensional integer grid.
 *
 * <p>Implementations must be <b>immutable</b>: every movement operation returns a
 * <b>new instance</b> rather than mutating the current one.</p>
 *
 * <p>This interface uses a self-referential generic parameter {@code <T>} (the
 * <i>curiously recurring template pattern</i>) so implementors can return their own type and
 * support fluent method chaining.</p>
 *
 * @param <T> the concrete implementing type, enabling fluent API returns
 */
public interface Movable<T extends Movable<T>> {

  /**
   * Returns the current absolute position of this entity in the grid.
   *
   * <p>The position typically refers to the entity’s origin or reference cell
   * (for example, the pivot of a tetromino).</p>
   *
   * @return the current {@link Cell} position (never {@code null})
   */
  Cell position();

  /**
   * Creates a <b>new instance</b> of this entity with the specified position.
   *
   * <p>This is the immutability hook for implementors: it leaves the current instance unchanged
   * and returns a copy with {@code newPosition}.</p>
   *
   * @param newPosition the new {@link Cell} position (must not be {@code null})
   *
   * @return a new instance of {@code T} with the updated position (never {@code null})
   */
  T withPosition(Cell newPosition);

  /**
   * Moves this entity by a relative offset.
   *
   * <p>The offset is applied by vector addition: {@code position() + delta}.</p>
   *
   * <p>Example:</p>
   * <pre>{@code
   * Cell delta = new Cell(2, -1);
   * T moved = entity.moveBy(delta);
   * }</pre>
   *
   * @param delta the relative movement as a {@link Cell} offset (must not be {@code null})
   *
   * @return a new instance of {@code T} at the updated position (never {@code null})
   */
  default T moveBy(Cell delta) {
    return withPosition(position().add(delta));
  }

  /**
   * Moves this entity one cell upward in the grid.
   *
   * <p>Decreases the y-coordinate by 1.</p>
   *
   * @return a new instance of {@code T} one cell up (never {@code null})
   */
  default T moveUp() {
    return moveBy(Cell.UP);
  }

  /**
   * Moves this entity one cell downward in the grid.
   *
   * <p>Increases the y-coordinate by 1.</p>
   *
   * @return a new instance of {@code T} one cell down (never {@code null})
   */
  default T moveDown() {
    return moveBy(Cell.DOWN);
  }

  /**
   * Moves this entity one cell to the left in the grid.
   *
   * <p>Decreases the x-coordinate by 1.</p>
   *
   * @return a new instance of {@code T} one cell left (never {@code null})
   */
  default T moveLeft() {
    return moveBy(Cell.LEFT);
  }

  /**
   * Moves this entity one cell to the right in the grid.
   *
   * <p>Increases the x-coordinate by 1.</p>
   *
   * @return a new instance of {@code T} one cell right (never {@code null})
   */
  default T moveRight() {
    return moveBy(Cell.RIGHT);
  }
}