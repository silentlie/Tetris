package com.example.tetris.core.piece;

import com.example.tetris.core.board.Cell;
import java.util.List;

/**
 * Single Tetris tetromino composed of four {@link Cell} blocks arranged as one of the seven
 * standard {@link TetrominoType} shapes.
 *
 * <p>Coordinates use screen-style axes: X increases to the right; Y increases downward.</p>
 *
 * <p>Each tetromino has:</p>
 * <ul>
 *   <li>a {@link TetrominoType} defining its shape (and typically its color),</li>
 *   <li>a {@link Cell} {@link #position()} denoting its origin/pivot in the grid,</li>
 *   <li>a {@link Rotation} describing its current orientation.</li>
 * </ul>
 *
 * <p>This record implements {@link Movable} and {@link Rotatable}. Instances are
 * <b>immutable</b>: movement and rotation methods return a <b>new</b> {@code Tetromino}.</p>
 *
 * @param type     the shape/type of this tetromino (must not be {@code null})
 * @param position the origin (pivot) of the tetromino within the grid (must not be {@code null})
 * @param rotation the current rotation state (must not be {@code null})
 */
public record Tetromino(TetrominoType type, Cell position, Rotation rotation)
    implements Movable<Tetromino>, Rotatable<Tetromino> {

  /**
   * Returns a new {@code Tetromino} with the specified position, keeping the same
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
   * Returns a new {@code Tetromino} with the specified rotation, keeping the same
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
   * Computes the absolute board coordinates of all four blocks of this tetromino.
   *
   * <p>The relative shape for {@link #type()} and {@link #rotation()} is translated by
   * {@link #position()} to produce absolute {@link Cell} coordinates.</p>
   *
   * <p>Example:</p>
   * <pre>{@code
   * Tetromino t = new Tetromino(TetrominoType.T, new Cell(4, 0), Rotation.RIGHT);
   * List<Cell> cells = t.getAbsoluteShape(); // absolute block positions
   * }</pre>
   *
   * @return an immutable list of four absolute {@link Cell} coordinates (never {@code null})
   *
   * @implNote This method allocates a new list on each call.
   */
  public List<Cell> getAbsoluteShape() {
    return type.relativeShape(rotation).stream().map(position::add).toList();
  }

  /**
   * Computes the axis-aligned bounding box of this tetromino in absolute grid coordinates.
   *
   * <p>The relative {@link Bound} for {@link #type()} and {@link #rotation()} is translated by
   * {@link #position()} to produce an absolute bounding rectangle.</p>
   *
   * <p>Example:</p>
   * <pre>{@code
   * Tetromino t = new Tetromino(TetrominoType.I, new Cell(5, -1), Rotation.UP);
   * Bound box = t.getAbsoluteBound(); // inclusive min/max X/Y covering the 4 blocks
   * }</pre>
   *
   * @return the absolute {@link Bound} of this tetromino (never {@code null})
   */
  public Bound getAbsoluteBound() {
    return type.relativeBound(rotation).translate(position.x(), position.y());
  }
}
