package com.example.tetris.core.state;

import com.example.tetris.core.piece.Rotation;
import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.piece.TetrominoType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable, minimal snapshot of a Tetris board suitable for AI evaluation, serialization, and
 * deterministic testing.
 *
 * <h2>Coordinate system</h2>
 * Screen-style axes: X increases to the right; Y increases downward. All coordinates referenced by
 * this type are absolute grid positions in that system.
 *
 * <h2>Contents</h2>
 * The snapshot captures:
 * <ul>
 *   <li>{@code width}/{@code height}: board dimensions in cells,</li>
 *   <li>{@code cells}: a 2D raster where {@code 0}=empty and non-zero=filled,</li>
 *   <li>{@code tetromino}: the currently active falling piece (maybe {@code null}),</li>
 *   <li>{@code next}: the next piece type in the queue (maybe {@code null}).</li>
 * </ul>
 *
 * <h2>Immutability</h2>
 * The record is immutable; however, {@code cells} is a nested array and thus mutable by reference.
 *
 * <h2>Serialization</h2>
 * Derived “shape” views of the current and next piece are exposed for JSON as
 * {@code "currentShape"} and {@code "nextShape"} via {@link #currentShape()} and
 * {@link #nextShape()}. These matrices are computed on demand and are not part of equality.
 *
 * <h2>Equality &amp; hashing</h2>
 * Equality compares dimensions, a deep element-wise comparison of {@code cells}, and the piece
 * fields; see {@link #equals(Object)}. {@link #hashCode()} is consistent with equality.
 *
 * <h2>Invariants (expected by callers)</h2>
 * <ul>
 *   <li>{@code width > 0}, {@code height > 0},</li>
 *   <li>{@code cells.length == height} and {@code cells[y].length == width} for all rows,</li>
 *   <li>values in {@code cells} use {@code 0} for empty (non-zero may encode IDs/colors).</li>
 * </ul>
 */
public record PureGame(
    int width,
    int height,
    int[][] cells,
    Tetromino tetromino,
    TetrominoType next
) {

  /**
   * Shape matrix of the currently active tetromino in its current rotation, expressed as a dense
   * 0/1 matrix with origin at the piece’s pivot.
   *
   * <p>Exposed for JSON as {@code "currentShape"}.</p>
   *
   * @return a new 2D matrix describing the current piece shape, or {@code 0×0} if
   *     {@code tetromino == null}
   */
  @JsonProperty("currentShape")
  public int[][] currentShape() {
    if (tetromino == null) {
      return new int[0][0];
    }
    return tetromino.type().relativeDerived(tetromino.rotation()).toShapeMatrix();
  }

  /**
   * Shape matrix of the next tetromino type in the queue, in the {@link Rotation#UP} orientation,
   * expressed as a dense 0/1 matrix with origin at the canonical pivot.
   *
   * <p>Exposed for JSON as {@code "nextShape"}.</p>
   *
   * @return a new 2D matrix describing the next piece shape, or {@code 0×0} if {@code next == null}
   */
  @JsonProperty("nextShape")
  public int[][] nextShape() {
    if (next == null) {
      return new int[0][0];
    }
    return next.relativeDerived(Rotation.UP).toShapeMatrix();
  }

  /**
   * Structural equality for snapshots.
   *
   * <p>Two {@code PureGame} instances are equal iff:</p>
   * <ul>
   *   <li>{@code width} and {@code height} are equal,</li>
   *   <li>{@code cells} is deeply equal via {@link Arrays#deepEquals(Object[], Object[])},</li>
   *   <li>{@code tetromino} and {@code next} are equal via {@link Objects#equals(Object, Object)}.
   *   </li>
   * </ul>
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof PureGame(int w, int h, int[][] c, Tetromino t, TetrominoType n)) {
      return width == w
          && height == h
          && Arrays.deepEquals(cells, c)
          && Objects.equals(tetromino, t)
          && Objects.equals(next, n);
    }
    return false;
  }

  /**
   * Hash code consistent with {@link #equals(Object)}: combines dimensions, a deep hash of
   * {@code cells}, and the piece fields.
   */
  @Override
  public int hashCode() {
    int hashCells = Arrays.deepHashCode(cells);
    return Objects.hash(width, height, hashCells, tetromino, next);
  }

  /**
   * String form including dimensions, the {@code cells} raster, and derived shape matrices for the
   * current and next pieces.
   */
  @Override
  public @NotNull String toString() {
    return "PureGame{"
        + "width=" + width
        + ", height=" + height
        + ", cells=" + Arrays.deepToString(cells)
        + ", currentShape=" + Arrays.deepToString(currentShape())
        + ", nextShape=" + Arrays.deepToString(nextShape())
        + '}';
  }
}
