package com.example.tetris.models;

/**
 * Represents an immutable contract for entities that can have a {@link Rotation} state.
 *
 * <p>Implementations of this interface must be <b>immutable</b>:
 * all rotation-related operations must return a <b>new instance</b> rather than mutating the
 * current one.</p>
 *
 * <p>This interface uses a self-referential generic type {@code <T>} to enable
 * <i>fluent method chaining</i> in subclasses or implementing records.</p>
 *
 * @param <T> the concrete implementing type, enabling fluent API returns
 */
public interface Rotatable<T extends Rotatable<T>> {

  /**
   * Returns the current {@link Rotation} state of this instance.
   *
   * @return the current rotation state, never {@code null}
   */
  Rotation rotation();

  /**
   * Creates a <b>new instance</b> with the specified {@link Rotation}.
   *
   * <p>This method acts as an <b>immutability hook</b>, ensuring that the
   * existing instance remains unchanged while returning a new one.</p>
   *
   * @param newRotation the new {@link Rotation} to apply, must not be {@code null}
   *
   * @return a new instance with the updated rotation, never {@code null}
   */
  T withRotation(Rotation newRotation);

  /**
   * Returns a new instance rotated 90 degrees <b>clockwise</b>.
   *
   * <p>This is equivalent to calling:
   * <pre>
   *   withRotation(rotation().rotateClockwise());
   * </pre>
   * </p>
   *
   * @return a new instance rotated clockwise, never {@code null}
   *
   * @see Rotation#rotateClockwise()
   */
  default T rotateClockwise() {
    return withRotation(rotation().rotateClockwise());
  }

  /**
   * Returns a new instance rotated 90 degrees <b>counter-clockwise</b>.
   *
   * <p>This is equivalent to calling:
   * <pre>
   *   withRotation(rotation().rotateCounterClockwise());
   * </pre>
   * </p>
   *
   * @return a new instance rotated counter-clockwise, never {@code null}
   *
   * @see Rotation#rotateCounterClockwise()
   */
  default T rotateCounterClockwise() {
    return withRotation(rotation().rotateCounterClockwise());
  }
}
