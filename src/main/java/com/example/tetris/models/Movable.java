package com.example.tetris.models;

/**
 * Represents an immutable contract for entities that can move within a grid.
 *
 * <p>Implementations of this interface must be <b>immutable</b>: any movement
 * operation should return a <i>new instance</i> with the updated position rather than modifying the
 * current one.
 *
 * @param <T> the concrete self type
 */
public interface Movable<T extends Movable<T>> {

  /**
   * Returns the current absolute position of this entity.
   *
   * <p>The position typically refers to the "origin" or "reference cell"
   * of the entity in the grid.
   *
   * @return the current {@link Cell} position
   */
  Cell position();

  /**
   * Creates a new instance of this entity with the given position.
   *
   * <p>This method acts as an immutability hook: implementations must
   * not mutate the current instance but instead return a new instance.
   *
   * @param newPosition the new {@link Cell} position
   *
   * @return a new instance of {@code T} with the updated position
   */
  T withPosition(Cell newPosition);

  /**
   * Moves this entity by a relative offset defined by the given delta.
   *
   * <p>The delta is applied by adding it to the current position.
   *
   * @param delta the relative movement as a {@link Cell} offset
   *
   * @return a new instance of {@code T} with the updated position
   */
  default T moveBy(Cell delta) {
    return withPosition(position().add(delta));
  }

  /**
   * Moves this entity one cell upward in the grid.
   *
   * <p>This decreases the y-coordinate by 1.
   *
   * @return a new instance of {@code T} one cell up
   */
  default T moveUp() {
    return moveBy(new Cell(0, -1));
  }

  /**
   * Moves this entity one cell downward in the grid.
   *
   * <p>This increases the y-coordinate by 1.
   *
   * @return a new instance of {@code T} one cell down
   */
  default T moveDown() {
    return moveBy(new Cell(0, 1));
  }

  /**
   * Moves this entity one cell to the left in the grid.
   *
   * <p>This decreases the x-coordinate by 1.
   *
   * @return a new instance of {@code T} one cell left
   */
  default T moveLeft() {
    return moveBy(new Cell(-1, 0));
  }

  /**
   * Moves this entity one cell to the right in the grid.
   *
   * <p>This increases the x-coordinate by 1.
   *
   * @return a new instance of {@code T} one cell right
   */
  default T moveRight() {
    return moveBy(new Cell(1, 0));
  }
}
