package com.example.tetris.core.piece;

/**
 * Immutable contract for entities that expose a {@link Rotation} state.
 *
 * <p>Implementations must be <b>immutable</b>: all rotation operations return a
 * <b>new instance</b> rather than mutating the current one.</p>
 *
 * <p>This interface uses a self-referential generic parameter {@code <T>} (the
 * “curiously recurring template pattern”) so implementors can return their own type and support
 * fluent method chaining.</p>
 *
 * @param <T> the concrete implementing type, enabling fluent API returns
 */
public interface Rotatable<T extends Rotatable<T>> {

  /**
   * Returns the current {@link Rotation} state of this instance.
   *
   * @return the current rotation state (never {@code null})
   */
  Rotation rotation();

  /**
   * Creates a <b>new instance</b> with the specified {@link Rotation}.
   *
   * <p>Acts as the immutability hook: leaves the current instance unchanged and returns a copy
   * with {@code newRotation}.</p>
   *
   * @param newRotation the rotation to apply (must not be {@code null})
   *
   * @return a new instance with the updated rotation (never {@code null})
   */
  T withRotation(Rotation newRotation);

  /**
   * Returns a new instance rotated 90 degrees <b>clockwise</b>.
   *
   * <p>This is equivalent to calling:</p>
   * <pre>{@code
   * withRotation(rotation().rotateClockwise());
   * }</pre>
   *
   * @return a new instance rotated clockwise (never {@code null})
   *
   * @see Rotation#rotateClockwise()
   */
  default T rotateClockwise() {
    return withRotation(rotation().rotateClockwise());
  }

  /**
   * Returns a new instance rotated 90 degrees <b>counter-clockwise</b>.
   *
   * <p>This is equivalent to calling:</p>
   * <pre>{@code
   * withRotation(rotation().rotateCounterClockwise());
   * }</pre>
   *
   * @return a new instance rotated counter-clockwise (never {@code null})
   *
   * @see Rotation#rotateCounterClockwise()
   */
  default T rotateCounterClockwise() {
    return withRotation(rotation().rotateCounterClockwise());
  }
}

