package com.example.tetris.core.piece;

import com.example.tetris.core.board.Cell;
import java.util.List;
import java.util.Objects;

/**
 * Single Tetris tetromino composed of four {@link Cell} blocks arranged as one of the seven
 * standard {@link TetrominoType} shapes.
 *
 * <p><strong>Coordinates:</strong> screen-style axes where X increases to the right and
 * Y increases downward.</p>
 *
 * <p>Each tetromino has:</p>
 * <ul>
 *   <li>a {@link TetrominoType} defining its shape (and typically its color),</li>
 *   <li>a {@link Cell} {@link #position()} denoting its origin/pivot in the grid,</li>
 *   <li>a {@link Rotation} describing its current orientation.</li>
 * </ul>
 *
 * <p><strong>Immutability &amp; value semantics:</strong> This is a Java {@code record}.
 * Instances are immutable; movement/rotation methods return a <em>new</em> {@code Tetromino}.
 * Equality and hashing are based on {@code type}, {@code position}, and {@code rotation}.</p>
 *
 * @param type     the tetromino shape/type (must not be {@code null})
 * @param position the origin (pivot) of the tetromino within the grid (must not be {@code null})
 * @param rotation the current rotation state (must not be {@code null})
 */
public record Tetromino(TetrominoType type, Cell position, Rotation rotation)
    implements Movable<Tetromino>, Rotatable<Tetromino> {

  /**
   * <p>This ensures that no Tetromino instance can be created with
   * null values for type, position and rotation.
   * Using Objects.requireNonNull() enforces these constraints at the time of construction
   * and gives an error message if violated.</p>
   */
  public Tetromino{
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(position, "position");
    Objects.requireNonNull(rotation, "rotation");
  }

  /**
   * Returns a new {@code Tetromino} with {@code newPosition}, keeping the same
   * {@link TetrominoType} and {@link Rotation}.
   *
   * @param newPosition the new grid position (must not be {@code null})
   *
   * @return a new {@link Tetromino} with the updated position
   */
  @Override
  public Tetromino withPosition(Cell newPosition) {
    return new Tetromino(this.type, newPosition, this.rotation);
  }

  /**
   * Returns a new {@code Tetromino} with {@code newRotation}, keeping the same
   * {@link TetrominoType} and {@link Cell} position.
   *
   * @param newRotation the rotation to apply (must not be {@code null})
   *
   * @return a new {@link Tetromino} with the updated rotation
   */
  @Override
  public Tetromino withRotation(Rotation newRotation) {
    return new Tetromino(this.type, this.position, newRotation);
  }

  /**
   * Computes the absolute grid coordinates of all four blocks of this tetromino.
   *
   * <p>The relative shape for {@link #type()} at {@link #rotation()} is translated by
   * {@link #position()} to produce absolute {@link Cell} coordinates.</p>
   *
   * <p>Example:</p>
   * <pre>{@code
   * Tetromino t = new Tetromino(TetrominoType.T, new Cell(4, 0), Rotation.RIGHT);
   * List<Cell> cells = t.getAbsoluteShape(); // four absolute block positions
   * }</pre>
   *
   * @return an unmodifiable list of exactly four absolute {@link Cell} coordinates (never
   *     {@code null})
   *
   * @implNote This method allocates a new list on each call.
   */
  public List<Cell> getAbsoluteShape() {
    return type.relativeShape(rotation).stream().map(position::add).toList();
  }

  /**
   * Computes the axis-aligned bounding box of this tetromino in absolute grid coordinates.
   *
   * <p>The relative {@link Bound} for {@link #type()} at {@link #rotation()} is translated by
   * {@link #position()} to produce an absolute, inclusive bounding rectangle.</p>
   *
   * <p>Example:</p>
   * <pre>{@code
   * Tetromino t = new Tetromino(TetrominoType.I, new Cell(5, -1), Rotation.UP);
   * Bound box = t.getAbsoluteBound(); // inclusive min/max X/Y covering the 4 blocks
   * }</pre>
   *
   * @return the absolute {@link Bound} of this tetromino (never {@code null})
   *
   * @implNote Runs in {@code O(1)}; no allocation beyond the returned {@code Bound}.
   */
  public Bound getAbsoluteBound() {
    return type.relativeBound(rotation).translate(position.x(), position.y());
  }
}
