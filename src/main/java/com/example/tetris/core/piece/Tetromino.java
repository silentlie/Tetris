package com.example.tetris.core.piece;

import com.example.tetris.core.board.Cell;
import java.util.List;
import java.util.Objects;

/**
 * A single Tetris tetromino composed of four {@link Cell} blocks arranged as one of the seven
 * standard {@link TetrominoType} shapes.
 *
 * <h2>Coordinate system</h2>
 *
 * <p>Uses screen-style axes: X increases to the right; Y increases downward.</p>
 *
 * <h2>State</h2>
 * <ul>
 *   <li>{@link #type()} – the canonical tetromino shape (and typically its color)</li>
 *   <li>{@link #position()} – the origin/pivot on the grid; all relative cells are
 *       translated by this origin to obtain absolute coordinates</li>
 *   <li>{@link #rotation()} – the current orientation</li>
 * </ul>
 *
 * <h2>Immutability &amp; value semantics</h2>
 *
 * <p>This is a Java {@code record}. Instances are immutable; movement/rotation methods return
 * a <em>new</em> {@code Tetromino}. Equality and hashing are derived from {@code type},
 * {@code position}, and {@code rotation}.</p>
 *
 * <p>Example (composition-friendly API):</p>
 * <pre>{@code
 * Tetromino t0 = new Tetromino(TetrominoType.T, new Cell(4, 0), Rotation.UP);
 * Tetromino t1 = t0.withPosition(new Cell(5, 1)).withRotation(Rotation.RIGHT);
 * }</pre>
 *
 * @param type     the tetromino shape/type (must not be {@code null})
 * @param position the origin/pivot of the tetromino within the grid (must not be {@code null})
 * @param rotation the current rotation state (must not be {@code null})
 *
 * @see TetrominoType
 * @see Rotation
 * @see Movable
 * @see Rotatable
 */
public record Tetromino(TetrominoType type, Cell position, Rotation rotation)
    implements Movable<Tetromino>, Rotatable<Tetromino> {

  /**
   * Canonical constructor enforcing non-null components.
   *
   * @throws NullPointerException if any component is {@code null}
   */
  public Tetromino {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(position, "position must not be null");
    Objects.requireNonNull(rotation, "rotation must not be null");
  }

  /**
   * Returns a new {@code Tetromino} at {@code newPosition}, preserving {@link #type()} and
   * {@link #rotation()}.
   *
   * @param newPosition the new grid origin/pivot (must not be {@code null})
   *
   * @return a new {@link Tetromino} with the updated position
   *
   * @throws NullPointerException if {@code newPosition} is {@code null}
   * @see Movable#withPosition(Cell)
   */
  @Override
  public Tetromino withPosition(Cell newPosition) {
    Objects.requireNonNull(newPosition, "newPosition must not be null");
    return new Tetromino(this.type, newPosition, this.rotation);
  }

  /**
   * Returns a new {@code Tetromino} with {@code newRotation}, preserving {@link #type()} and
   * {@link #position()}.
   *
   * @param newRotation the rotation to apply (must not be {@code null})
   *
   * @return a new {@link Tetromino} with the updated rotation
   *
   * @throws NullPointerException if {@code newRotation} is {@code null}
   * @see Rotatable#withRotation(Rotation)
   */
  @Override
  public Tetromino withRotation(Rotation newRotation) {
    Objects.requireNonNull(newRotation, "newRotation must not be null");
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
   * @return an unmodifiable list of exactly four absolute {@link Cell} coordinates
   *
   * @implNote Allocates a new list on each call; complexity is O(1).
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
   * @return the absolute {@link Bound} of this tetromino
   *
   * @implNote Runs in O(1); only the returned {@code Bound} is allocated.
   */
  public Bound getAbsoluteBound() {
    return type.relativeBound(rotation).translate(position.x(), position.y());
  }
}
