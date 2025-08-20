package com.example.tetris.models;

import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a single tetromino piece in the game of Tetris.
 *
 * <p>A tetromino is defined by three attributes:</p>
 * <ul>
 *   <li>{@link TetrominoType type} — the shape of the piece (I, O, T, S, Z, J, L)</li>
 *   <li>{@link Rotation rotation} — the current orientation (UP, RIGHT, DOWN, LEFT)</li>
 *   <li>{@link Cell position} — the absolute position of the tetromino on the board,
 *       corresponding to the origin cell of its shape</li>
 * </ul>
 *
 * <p>This class is immutable: all movement and rotation operations return new
 * {@code Tetromino} instances instead of mutating the existing one.</p>
 */
public record Tetromino(TetrominoType type, Rotation rotation, Cell position) {

  /**
   * Constructs a new tetromino with a random type and random rotation, positioned at the given
   * cell.
   *
   * @param position the starting cell position of the tetromino
   */
  public Tetromino(Cell position) {
    this(TetrominoType.randomType(), Rotation.randomRotation(), position);
  }

  /**
   * Moves this tetromino one cell upward.
   *
   * @return a new tetromino positioned one row above
   */
  public Tetromino moveUp() {
    return new Tetromino(type, rotation, position.moveUp());
  }

  /**
   * Moves this tetromino one cell downward.
   *
   * @return a new tetromino positioned one row below
   */
  public Tetromino moveDown() {
    return new Tetromino(type, rotation, position.moveDown());
  }

  /**
   * Moves this tetromino one cell to the left.
   *
   * @return a new tetromino positioned one column left
   */
  public Tetromino moveLeft() {
    return new Tetromino(type, rotation, position.moveLeft());
  }

  /**
   * Moves this tetromino one cell to the right.
   *
   * @return a new tetromino positioned one column right
   */
  public Tetromino moveRight() {
    return new Tetromino(type, rotation, position.moveRight());
  }

  /**
   * Rotates this tetromino 90 degrees clockwise.
   *
   * @return a new tetromino with updated rotation
   */
  public Tetromino rotateClockwise() {
    return new Tetromino(type, rotation.rotateClockwise(), position);
  }

  /**
   * Rotates this tetromino 90 degrees counter-clockwise.
   *
   * @return a new tetromino with updated rotation
   */
  public Tetromino rotateCounterClockwise() {
    return new Tetromino(type, rotation.rotateCounterClockwise(), position);
  }

  /**
   * Returns the relative coordinates of this tetromino’s cells in its current rotation.
   *
   * <p>The coordinates are relative to the tetromino’s origin cell ({@link #position}).</p>
   *
   * @return an array of relative {@link Cell} offsets
   */
  public Cell[] getRelativeShape() {
    return SHAPES[type.ordinal()][rotation.ordinal()].clone();
  }

  /**
   * Returns the absolute coordinates of this tetromino’s cells on the board.
   *
   * <p>This is computed by adding the tetromino’s {@link #position} to each relative
   * cell in its shape.</p>
   *
   * @return an array of absolute {@link Cell} positions
   */
  public Cell[] getAbsoluteShape() {
    Cell[] rel = getRelativeShape();
    Cell[] abs = new Cell[rel.length];
    for (int i = 0; i < rel.length; i++) {
      abs[i] = position.add(rel[i]);
    }
    return abs;
  }

  private static final Cell[][][] SHAPES =
      new Cell[TetrominoType.values().length][Rotation.values().length][];

  static {
    // Define each tetromino in the UP rotation
    final Map<TetrominoType, Cell[]> base = new EnumMap<>(TetrominoType.class);
    base.put(TetrominoType.I, new Cell[]{
        new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(2, 0)
    });
    base.put(TetrominoType.O, new Cell[]{
        new Cell(0, 0), new Cell(1, 0), new Cell(0, 1), new Cell(1, 1)
    });
    base.put(TetrominoType.T, new Cell[]{
        new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(0, 1)
    });
    base.put(TetrominoType.S, new Cell[]{
        new Cell(0, 0), new Cell(1, 0), new Cell(-1, 1), new Cell(0, 1)
    });
    base.put(TetrominoType.Z, new Cell[]{
        new Cell(-1, 0), new Cell(0, 0), new Cell(0, 1), new Cell(1, 1)
    });
    base.put(TetrominoType.J, new Cell[]{
        new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(-1, 1)
    });
    base.put(TetrominoType.L, new Cell[]{
        new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(1, 1)
    });
    // Precompute rotated shapes
    for (TetrominoType t : TetrominoType.values()) {
      Cell[] up = base.get(t);
      for (Rotation r : Rotation.values()) {
        SHAPES[t.ordinal()][r.ordinal()] = (t == TetrominoType.O) ? up : rotate(up, r);
      }
    }
  }

  /**
   * Rotates a set of relative cells into the given rotation.
   *
   * <p>Rotation is clockwise on screen (y-axis pointing downward).
   * The transformation per step is {@code (x, y) → (-y, x)}.</p>
   *
   * @param up the base cells in the UP orientation
   * @param r  the desired rotation
   * @return the rotated array of cells
   */
  private static Cell[] rotate(Cell[] up, Rotation r) {
    int steps = switch (r) {
      case UP -> 0;
      case RIGHT -> 1;
      case DOWN -> 2;
      case LEFT -> 3;
    };
    Cell[] out = new Cell[up.length];
    for (int i = 0; i < up.length; i++) {
      int x = up[i].x();
      int y = up[i].y();
      for (int k = 0; k < steps; k++) {
        int ny = x;
        x = -y;
        y = ny;
      }
      out[i] = new Cell(x, y);
    }
    return out;
  }
}