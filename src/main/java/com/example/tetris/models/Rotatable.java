package com.example.tetris.models;

/**
 * Represents an immutable contract for entities that have a {@link Rotation} state.
 *
 * <p>Implementations of this interface must be <b>immutable</b>:
 * all rotation-related operations must return a <b>new instance</b> rather than modifying the
 * current one.</p>
 *
 * <p>This interface uses a self-referential generic type {@code <T>} to support
 * <i>fluent method chaining</i> in subclasses or implementations.</p>
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
   * Creates a <b>new instance</b> with the specified rotation.
   *
   * <p>This method acts as an immutability hook, ensuring that
   * the existing instance remains unchanged.</p>
   *
   * @param newRotation the new rotation to apply, must not be {@code null}
   *
   * @return a new instance with the updated rotation
   */
  T withRotation(Rotation newRotation);

  /**
   * Returns a new instance rotated 90 degrees <b>clockwise</b>.
   *
   * @return a new rotated instance, never {@code null}
   */
  default T rotateClockwise() {
    return withRotation(rotation().rotateClockwise());
  }

  /**
   * Returns a new instance rotated 90 degrees <b>counter-clockwise</b>.
   *
   * @return a new rotated instance, never {@code null}
   */
  default T rotateCounterClockwise() {
    return withRotation(rotation().rotateCounterClockwise());
  }
}