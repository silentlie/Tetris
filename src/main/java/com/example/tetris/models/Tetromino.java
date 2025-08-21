package com.example.tetris.models;

import java.util.List;
import java.util.Map;

/**
 * Represents a Tetris tetromino, which consists of four blocks arranged in one of seven shapes.
 * Each tetromino has a specific type, position on the grid, and rotation.
 *
 * <p>This record implements {@link Movable} and {@link Rotatable}, allowing the tetromino to be
 * moved and rotated while maintaining immutability.</p>
 *
 * @param type     the shape type of the tetromino
 * @param position the current origin position of the tetromino on the grid
 * @param rotation the current rotation of the tetromino
 */
public record Tetromino(TetrominoType type, Cell position, Rotation rotation)
    implements Movable<Tetromino>, Rotatable<Tetromino> {

  /**
   * Shape definitions for each {@link TetrominoType} in the default {@link Rotation#UP}
   * orientation. Each shape is defined as a list of {@link Cell} offsets relative to an origin at
   * (0, 0).
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
   * Creates a random tetromino at the given spawn position with the default {@link Rotation#UP}.
   *
   * @param spawnPosition the initial grid position for the tetromino
   *
   * @return a new {@link Tetromino} with a random type and an upward orientation
   */
  public static Tetromino randomTetromino(Cell spawnPosition) {
    return new Tetromino(TetrominoType.randomType(), spawnPosition, Rotation.UP);
  }

  /**
   * Returns a new tetromino with the specified position, keeping the same type and rotation.
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
   * Returns a new tetromino with the specified rotation, keeping the same type and position.
   *
   * @param newRotation the new rotation
   *
   * @return a new {@link Tetromino} instance with the updated rotation
   */
  @Override
  public Tetromino withRotation(Rotation newRotation) {
    return new Tetromino(this.type, this.position, newRotation);
  }

  public List<Cell> getAbsoluteShape() {
    return SHAPES.get(type).stream()
        .map(rotation::apply)
        .map(position::add)
        .toList();
  }
}
