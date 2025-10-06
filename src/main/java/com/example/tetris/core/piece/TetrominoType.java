package com.example.tetris.core.piece;

import com.example.tetris.core.board.Cell;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.scene.paint.Color;

/**
 * The seven standard Tetris tetromino types: {@code I}, {@code O}, {@code T}, {@code S}, {@code Z},
 * {@code J}, and {@code L}.
 *
 * <p>Each enum constant stores a display {@link Color} and a canonical shape expressed as four
 * {@link Cell} offsets relative to an origin at {@code (0, 0)}. Coordinates follow the typical
 * screen/grid orientation: X increases to the right; Y increases downward.</p>
 *
 * <p>Utility methods let you query the display color, derive the rotated shape as relative
 * coordinates, and obtain an inclusive axis-aligned {@link Bound} for any {@link Rotation}. Callers
 * typically translate the relative shape by a tetromino’s board position to get absolute
 * coordinates.</p>
 *
 * <h2>Thread-safety</h2>
 * Derived (rotation-specific) data is cached in a thread-safe {@link ConcurrentHashMap}. The enum
 * is thread-safe; returned collections are unmodifiable.
 */
public enum TetrominoType {

  /**
   * Straight line tetromino (“I”).
   */
  I(Color.CYAN, List.of(new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(2, 0))),

  /**
   * Square tetromino (“O”).
   */
  O(Color.YELLOW, List.of(new Cell(0, 0), new Cell(1, 0), new Cell(0, 1), new Cell(1, 1))),

  /**
   * T-shaped tetromino (“T”).
   */
  T(Color.MAGENTA, List.of(new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(0, 1))),

  /**
   * Skew tetromino leaning right (“S”).
   */
  S(Color.GREEN, List.of(new Cell(0, 0), new Cell(1, 0), new Cell(-1, 1), new Cell(0, 1))),

  /**
   * Skew tetromino leaning left (“Z”).
   */
  Z(Color.RED, List.of(new Cell(-1, 0), new Cell(0, 0), new Cell(0, 1), new Cell(1, 1))),

  /**
   * Left-facing L tetromino (“J”).
   */
  J(Color.BLUE, List.of(new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(-1, 1))),

  /**
   * Right-facing L tetromino (“L”).
   */
  L(Color.ORANGE, List.of(new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(1, 1)));

  private static final Map<Key, Derived> CACHE = new ConcurrentHashMap<>();
  private final Color color;
  private final List<Cell> shape;

  TetrominoType(Color color, List<Cell> shape) {
    this.color = color;
    this.shape = shape;
  }

  /**
   * Computes rotation-specific derived data for a given type and rotation.
   *
   * <p>The canonical relative coordinates are transformed by {@code r}, then the inclusive
   * {@link Bound} is computed across the rotated coordinates.</p>
   *
   * @param t the tetromino type
   * @param r the rotation to apply
   *
   * @return a new {@link Derived} holding the rotated coordinates and their bound
   */
  private static Derived computeRelative(TetrominoType t, Rotation r) {
    List<Cell> rel = t.shape.stream().map(r::apply).toList();
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;
    for (Cell c : rel) {
      int x = c.x();
      int y = c.y();
      if (x < minX) {
        minX = x;
      }
      if (y < minY) {
        minY = y;
      }
      if (x > maxX) {
        maxX = x;
      }
      if (y > maxY) {
        maxY = y;
      }
    }
    return new Derived(List.copyOf(rel), new Bound(minX, minY, maxX, maxY));
  }

  /**
   * Returns the display {@link Color} associated with this tetromino type.
   *
   * <p>Typically used by the renderer to color blocks for this type.</p>
   *
   * @return the color for this tetromino type (never {@code null})
   */
  public Color getColor() {
    return color;
  }

  /**
   * Returns cached rotation-specific data for this type.
   *
   * <p>Uses a thread-safe cache keyed by (type, rotation). The returned {@link Derived} contains
   * both the rotated relative coordinates and their inclusive {@link Bound}.</p>
   *
   * @param r the desired rotation (must not be {@code null})
   *
   * @return the cached-or-computed {@link Derived} for this type and rotation
   */
  public Derived relativeDerived(Rotation r) {
    return CACHE.computeIfAbsent(new Key(this, r), k -> computeRelative(k.type, k.rotation));
  }

  /**
   * Returns the rotated relative shape for this type.
   *
   * <p>The list contains four {@link Cell} offsets relative to an origin; translate by a tetromino
   * position to obtain absolute coordinates on the board.</p>
   *
   * @param r the desired rotation (must not be {@code null})
   *
   * @return an unmodifiable list of four relative coordinates (never {@code null})
   */
  public List<Cell> relativeShape(Rotation r) {
    return relativeDerived(r).relative;
  }

  /**
   * Returns the inclusive axis-aligned bound of the rotated relative shape.
   *
   * <p>The bound is expressed in the same relative coordinate system as
   * {@link #relativeShape(Rotation)}.</p>
   *
   * @param r the desired rotation (must not be {@code null})
   *
   * @return the inclusive bound of the rotated shape (never {@code null})
   */
  public Bound relativeBound(Rotation r) {
    return relativeDerived(r).bound;
  }

  /**
   * Cache key combining tetromino type and rotation.
   */
  private record Key(TetrominoType type, Rotation rotation) {

  }

  /**
   * Rotation-specific derived data for a tetromino type.
   *
   * <p>Contains the rotated relative {@link Cell} list and its inclusive {@link Bound} in the same
   * coordinate space.</p>
   *
   * @param relative the rotated relative coordinates (size 4)
   * @param bound    the inclusive bound spanning {@code relative}
   */
  public record Derived(List<Cell> relative, Bound bound) {

    /**
     * Returns a dense 0/1 matrix covering the rotated shape’s inclusive bound.
     *
     * <p>The matrix is indexed row-major as {@code bits[ny][nx]} where {@code ny} corresponds to
     * Y (rows) and {@code nx} to X (columns). The matrix has height {@code bound.height()} and
     * width {@code bound.width()}. A cell is set to {@code 1} iff the corresponding coordinate
     * within the bound contains a block; otherwise it is {@code 0}.</p>
     *
     * <p>Indexing example for mapping a relative cell {@code (x, y)} into the matrix:</p>
     * <pre>{@code
     * int nx = x - bound.minX();
     * int ny = y - bound.minY();
     * bits[ny][nx] = 1;
     * }</pre>
     *
     * @return a new 0/1 matrix representing the rotated shape within its bound (never {@code null})
     */
    public int[][] toShapeMatrix() {
      List<Cell> cells = relative;
      if (cells == null || cells.isEmpty()) {
        return new int[0][0];
      }
      int[][] bits = new int[bound.height()][bound.width()];
      for (Cell c : cells) {
        int nx = c.x() - bound.minX();
        int ny = c.y() - bound.minY();
        bits[ny][nx] = 1;
      }
      return bits;
    }
  }
}
