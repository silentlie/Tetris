package com.example.tetris.models;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents the four possible rotation states for a tetromino or other game element on the board.
 *
 * <p>The states are ordered clockwise:
 * {@link #UP}, {@link #RIGHT}, {@link #DOWN}, {@link #LEFT}.</p>
 */
public enum Rotation {
  UP,
  RIGHT,
  DOWN,
  LEFT;

  /**
   * Returns the next {@code Rotation} in clockwise order.
   *
   * <p>For example:</p>
   * <ul>
   *   <li>{@code UP → RIGHT}</li>
   *   <li>{@code RIGHT → DOWN}</li>
   *   <li>{@code DOWN → LEFT}</li>
   *   <li>{@code LEFT → UP}</li>
   * </ul>
   *
   * @return the rotation one step clockwise from this one
   */
  public Rotation rotateClockwise() {
    return values()[(this.ordinal() + 1) % 4];
  }

  /**
   * Returns the next {@code Rotation} in counter-clockwise order.
   *
   * <p>For example:</p>
   * <ul>
   *   <li>{@code UP → LEFT}</li>
   *   <li>{@code LEFT → DOWN}</li>
   *   <li>{@code DOWN → RIGHT}</li>
   *   <li>{@code RIGHT → UP}</li>
   * </ul>
   *
   * @return the rotation one step counter-clockwise from this one
   */
  public Rotation rotateCounterClockwise() {
    return values()[(this.ordinal() + 3) % 4];
  }

  /**
   * Returns a random {@code Rotation} value.
   *
   * @return a randomly chosen rotation state
   */
  public static Rotation randomRotation() {
    Rotation[] values = Rotation.values();
    return values[ThreadLocalRandom.current().nextInt(values.length)];
  }
}
