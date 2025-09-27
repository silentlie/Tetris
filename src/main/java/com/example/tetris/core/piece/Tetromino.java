package com.example.tetris.core.piece;

import com.example.tetris.core.board.Cell;
import java.util.List;
import java.util.Map;

/**
 * Represents a single Tetris tetromino, composed of four {@link Cell} blocks arranged into one of
 * the seven standard {@link TetrominoType} shapes.
 *
 * <p>Each tetromino has:
 * <ul>
 *   <li>a {@link TetrominoType} defining its shape and color</li>
 *   <li>a {@link Cell} {@link #position()} representing its origin (pivot) in the grid</li>
 *   <li>a {@link Rotation} state defining its current orientation</li>
 * </ul>
 *
 * <p>This record implements {@link Movable} and {@link Rotatable}, meaning
 * tetromino are <b>immutable</b>: any movement or rotation returns a
 * <b>new {@code Tetromino}</b> instance instead of mutating the existing one.</p>
 *
 * @param type     the {@link TetrominoType} defining the tetromino's shape and color
 * @param position the origin cell (pivot) of the tetromino in the grid
 * @param rotation the current {@link Rotation} of the tetromino
 */
public record Tetromino(TetrominoType type, Cell position, Rotation rotation)
    implements Movable<Tetromino>, Rotatable<Tetromino> {

  /**
   * Shape definitions for each {@link TetrominoType} in the default {@link Rotation#UP}
   * orientation.
   *
   * <p>Each tetromino shape is defined as a list of {@link Cell} offsets
   * relative to its pivot at {@code (0, 0)}. Rotations are applied dynamically when retrieving
   * absolute positions.</p>
   */
  private static final Map<TetrominoType, List<Cell>> SHAPES = Map.of(
      TetrominoType.I, List.of(new Cell(-2, 0), new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0)),
      TetrominoType.O, List.of(new Cell(0, 0), new Cell(1, 0), new Cell(0, 1), new Cell(1, 1)),
      TetrominoType.T, List.of(new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(0, 1)),
      TetrominoType.S, List.of(new Cell(0, 0), new Cell(1, 0), new Cell(-1, 1), new Cell(0, 1)),
      TetrominoType.Z, List.of(new Cell(-1, 0), new Cell(0, 0), new Cell(0, 1), new Cell(1, 1)),
      TetrominoType.J, List.of(new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(-1, 1)),
      TetrominoType.L, List.of(new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(1, 1))
  );

  /**
   * Creates a random {@code Tetromino} at the specified spawn position, using the default
   * {@link Rotation#UP}.
   *
   * <p>The tetromino type is randomly selected using
   * {@link TetrominoType#randomType()}.</p>
   *
   * @param spawnPosition the initial grid position of the tetromino's origin
   *
   * @return a new {@link Tetromino} with a random type and upward orientation
   */
  public static Tetromino randomTetromino(Cell spawnPosition) {
    return new Tetromino(TetrominoType.randomType(), spawnPosition, Rotation.UP);
  }

  /**
   * Returns a new {@code Tetromino} with the specified position, keeping the same
   * {@link TetrominoType} and {@link Rotation}.
   *
   * @param newPosition the new grid position
   *
   * @return a new {@link Tetromino} instance with the updated position
   */
  @Override
  public Tetromino withPosition(Cell newPosition) {
    return new Tetromino(this.type, newPosition, this.rotation);
  }

  /**
   * Returns a new {@code Tetromino} with the specified rotation, keeping the same
   * {@link TetrominoType} and {@link Cell} position.
   *
   * @param newRotation the new {@link Rotation} to apply
   *
   * @return a new {@link Tetromino} instance with the updated rotation
   */
  @Override
  public Tetromino withRotation(Rotation newRotation) {
    return new Tetromino(this.type, this.position, newRotation);
  }

  /**
   * Returns the list of absolute {@link Cell} positions occupied by this tetromino on the game
   * board, based on its {@link TetrominoType}, {@link Rotation}, and {@link #position()}.
   *
   * <p>This method first retrieves the shape offsets from {@link #SHAPES},
   * applies the current rotation via {@link Rotation#apply(Cell)}, and then translates them to the
   * tetromino's origin position.</p>
   *
   * <p>Example usage:</p>
   * <pre>
   *   Tetromino t = Tetromino.randomTetromino(new Cell(5, 0));
   *   List&lt;Cell&gt; cells = t.getAbsoluteShape();
   * </pre>
   *
   * @return a list of absolute {@link Cell} positions, never {@code null}
   */
  public List<Cell> getAbsoluteShape() {
    return SHAPES.get(type).stream()
        .map(rotation::apply)
        .map(position::add)
        .toList();
  }
}
