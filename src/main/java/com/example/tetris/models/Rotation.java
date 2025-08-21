package com.example.tetris.models;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents the four possible rotation states for tetrominoes or other rotatable entities within
 * the Tetris game grid.
 *
 * <p>The rotation states are ordered clockwise as follows:
 * {@link #UP} → {@link #RIGHT} → {@link #DOWN} → {@link #LEFT} → {@link #UP}.</p>
 *
 * <p>This enum also provides utility methods for rotating clockwise,
 * counter-clockwise, randomly selecting a rotation, and applying a rotation transformation to a
 * given {@link Cell}.</p>
 */
public enum Rotation {
  /**
   * Default orientation with no rotation applied.
   */
  UP,

  /**
   * Rotated 90° clockwise from {@link #UP}.
   */
  RIGHT,

  /**
   * Rotated 180° from {@link #UP}.
   */
  DOWN,

  /**
   * Rotated 270° clockwise (or 90° counter-clockwise) from {@link #UP}.
   */
  LEFT;

  /**
   * Returns the next {@code Rotation} in clockwise order.
   *
   * <p>Example transitions:</p>
   * <ul>
   *   <li>{@link #UP} → {@link #RIGHT}</li>
   *   <li>{@link #RIGHT} → {@link #DOWN}</li>
   *   <li>{@link #DOWN} → {@link #LEFT}</li>
   *   <li>{@link #LEFT} → {@link #UP}</li>
   * </ul>
   *
   * @return the rotation one step clockwise from this one, never {@code null}
   */
  public Rotation rotateClockwise() {
    return values()[(this.ordinal() + 1) % 4];
  }

  /**
   * Returns the next {@code Rotation} in counter-clockwise order.
   *
   * <p>Example transitions:</p>
   * <ul>
   *   <li>{@link #UP} → {@link #LEFT}</li>
   *   <li>{@link #LEFT} → {@link #DOWN}</li>
   *   <li>{@link #DOWN} → {@link #RIGHT}</li>
   *   <li>{@link #RIGHT} → {@link #UP}</li>
   * </ul>
   *
   * @return the rotation one step counter-clockwise from this one, never {@code null}
   */
  public Rotation rotateCounterClockwise() {
    return values()[(this.ordinal() + 3) % 4];
  }

  /**
   * Returns a random {@code Rotation} value.
   *
   * <p>This method uses {@link ThreadLocalRandom} to ensure
   * thread-safe randomization.</p>
   *
   * @return a randomly selected {@code Rotation}, never {@code null}
   */
  public static Rotation randomRotation() {
    Rotation[] values = Rotation.values();
    return values[ThreadLocalRandom.current().nextInt(values.length)];
  }

  /**
   * Applies this rotation to a given {@link Cell} relative offset.
   *
   * <p>The transformation assumes that the input {@code Cell} is expressed
   * in coordinates relative to an origin point (e.g. the tetromino's pivot). The returned
   * {@code Cell} represents the rotated coordinates.</p>
   *
   * <p>For example:</p>
   * <pre>
   *   Cell original = new Cell(1, 2);
   *   Cell rotated = Rotation.RIGHT.apply(original);
   * </pre>
   *
   * @param c the {@link Cell} to transform, must not be {@code null}
   *
   * @return a new {@link Cell} with rotated coordinates, never {@code null}
   */
  public Cell apply(Cell c) {
    return switch (this) {
      case UP -> c;
      case RIGHT -> new Cell(c.y(), -c.x());
      case DOWN -> new Cell(-c.x(), -c.y());
      case LEFT -> new Cell(-c.y(), c.x());
    };
  }
}
