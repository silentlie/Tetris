package com.example.tetris.core.piece;

import com.example.tetris.core.board.Cell;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Four rotation states for tetrominoes or other rotatable entities on the Tetris grid.
 *
 * <p>States are ordered clockwise:
 * {@link #UP} → {@link #RIGHT} → {@link #DOWN} → {@link #LEFT} → {@link #UP}.</p>
 *
 * <p>Utility methods are provided to rotate clockwise, rotate counter-clockwise, choose a random
 * rotation, and apply the rotation to a relative {@link Cell} offset.</p>
 */
public enum Rotation {
  /**
   * Default orientation with no rotation.
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
   * Rotated 270° clockwise (90° counter-clockwise) from {@link #UP}.
   */
  LEFT;

  /**
   * Returns a random {@code Rotation}.
   *
   * <p>Uses {@link ThreadLocalRandom} for thread-safe selection.</p>
   *
   * @return a randomly selected rotation (never {@code null})
   */
  public static Rotation randomRotation() {
    Rotation[] values = Rotation.values();
    return values[ThreadLocalRandom.current().nextInt(values.length)];
  }

  /**
   * Returns the next rotation in clockwise order.
   *
   * <p>Examples:</p>
   * <ul>
   *   <li>{@link #UP} → {@link #RIGHT}</li>
   *   <li>{@link #RIGHT} → {@link #DOWN}</li>
   *   <li>{@link #DOWN} → {@link #LEFT}</li>
   *   <li>{@link #LEFT} → {@link #UP}</li>
   * </ul>
   *
   * @return the rotation one step clockwise from this (never {@code null})
   */
  public Rotation rotateClockwise() {
    return values()[(ordinal() + 1) % 4];
  }

  /**
   * Returns the next rotation in counter-clockwise order.
   *
   * <p>Examples:</p>
   * <ul>
   *   <li>{@link #UP} → {@link #LEFT}</li>
   *   <li>{@link #LEFT} → {@link #DOWN}</li>
   *   <li>{@link #DOWN} → {@link #RIGHT}</li>
   *   <li>{@link #RIGHT} → {@link #UP}</li>
   * </ul>
   *
   * @return the rotation one step counter-clockwise from this (never {@code null})
   */
  public Rotation rotateCounterClockwise() {
    return values()[(ordinal() + 3) % 4];
  }

  /**
   * Applies this rotation to a relative {@link Cell} offset.
   *
   * <p>The input {@code Cell} is interpreted as coordinates relative to an origin
   * (for example, a tetromino pivot). The returned cell contains the rotated coordinates.</p>
   *
   * <p>Example:</p>
   * <pre>{@code
   * Cell original = new Cell(1, 2);
   * Cell rotated = Rotation.RIGHT.apply(original); // -> (2, -1)
   * }</pre>
   *
   * @param c the relative offset to transform (must not be {@code null})
   *
   * @return a new {@link Cell} with rotated coordinates (never {@code null})
   */
  public Cell apply(Cell c) {
    Objects.requireNonNull(c, "Cannot apply a null value");
    return switch (this) {
      case UP -> c;
      case RIGHT -> new Cell(c.y(), -c.x());
      case DOWN -> new Cell(-c.x(), -c.y());
      case LEFT -> new Cell(-c.y(), c.x());
    };
  }
}
