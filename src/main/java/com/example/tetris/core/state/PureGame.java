package com.example.tetris.core.state;

import com.example.tetris.core.board.Cell;
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
 * Methods that “mutate” the board (e.g., line clear, placement) return <em>new</em> matrices
 * created via {@link #deepCopy(int[][])} to preserve snapshot semantics.
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
 *
 * <h2>Typical use</h2>
 * <pre>{@code
 * PureGame pg = new PureGame(w, h, cells, active, nextType);
 * if (pg.fits(active.rotateCW())) {
 *   int[][] hypothetical = pg.placePiece(active.rotateCW());
 *   // feed hypothetical snapshot to AI...
 * }
 * }</pre>
 */
public record PureGame(
    int width,
    int height,
    int[][] cells,
    Tetromino tetromino,
    TetrominoType next
) {

  /**
   * Creates a deep copy of a 2D {@code int} matrix (row-major).
   *
   * <p>Each row is {@link Arrays#copyOf(int[], int) copied} independently, producing a
   * new outer array and new inner row arrays.</p>
   *
   * @param original source matrix; must be rectangular (all rows same length)
   *
   * @return a deep copy with identical dimensions and contents
   *
   * @throws NullPointerException if {@code original} or any row is {@code null}
   */
  public static int[][] deepCopy(int[][] original) {
    int[][] copy = new int[original.length][];
    for (int i = 0; i < original.length; i++) {
      copy[i] = Arrays.copyOf(original[i], original[i].length);
    }
    return copy;
  }

  /**
   * Returns a new board with all full rows removed and the remaining rows compacted downward
   * (classic Tetris “line clear”).
   *
   * <p><strong>Algorithm:</strong></p>
   * <ol>
   *   <li>Scan {@code original} top-to-bottom, detect full rows (no zeros).</li>
   *   <li>Copy non-full rows into a new buffer from the bottom up.</li>
   *   <li>Fill remaining top rows with empty rows (all zeros).</li>
   * </ol>
   *
   * <p>Time complexity: {@code O(rows × cols)}. Space complexity: {@code O(rows × cols)} for
   * the new matrix.</p>
   *
   * @param original source board matrix (not modified)
   *
   * @return a new matrix with full lines cleared and gapless compaction applied
   *
   * @throws NullPointerException if {@code original} or any row is {@code null}
   * @implNote Assumes a rectangular matrix (constant row length).
   */
  public static int[][] clearLines(int[][] original) {
    int[][] copy = new int[original.length][];
    int write = original.length - 1;

    for (int y = original.length - 1; y >= 0; y--) {
      int[] row = original[y];
      boolean full = true;
      for (int cell : row) {
        if (cell == 0) {
          full = false;
          break;
        }
      }
      if (!full) {
        copy[write--] = Arrays.copyOf(row, row.length);
      }
    }

    // Fill remaining rows with empty rows (all zeros)
    int cols = original.length > 0 ? original[0].length : 0;
    while (write >= 0) {
      copy[write--] = new int[cols];
    }
    return copy;
  }

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
   * Simulates a hard drop of {@code tetromino} and returns a new board with the piece locked and
   * any resulting line clears applied.
   *
   * <p><strong>Process:</strong> repeatedly attempt to move the piece one row down while
   * the next position {@link #fits(Tetromino)}. When the next move no longer fits, lock at the last
   * valid position and {@link #placePiece(Tetromino) place} it (which also clears lines).</p>
   *
   * <p>If {@code tetromino} is {@code null} or does not fit at its current position (e.g., spawn
   * overlap), this method returns a deep copy of {@link #cells} (no-op).</p>
   *
   * @param tetromino the piece to drop (its current position and rotation are respected)
   *
   * @return a new matrix with the piece written and full lines cleared; or an unchanged deep copy
   *     of {@link #cells} if the piece is {@code null} or cannot be placed
   *
   * @implNote Runs in {@code O(drop distance × 4)} since each tetromino has four cells. The
   *     board uses value {@code 1} for filled cells; adapt {@link #placePiece(Tetromino)} if you
   *     encode colors/IDs.
   */
  public int[][] hardDrop(Tetromino tetromino) {
    if (tetromino == null || !fits(tetromino)) {
      return deepCopy(cells);
    }
    for (Tetromino next = tetromino.moveDown(); fits(next); next = next.moveDown()) {
      tetromino = next;
    }
    return placePiece(tetromino);
  }

  /**
   * Tests whether a tetromino fits entirely within the board bounds and does not collide with any
   * occupied cell in {@link #cells}.
   *
   * <p><strong>Bounds rule:</strong> any negative X/Y or any X/Y beyond {@code [0,width)} /
   * {@code [0,height)} fails. Unlike some board-level fit checks, positions above the top (negative
   * Y) are <em>not</em> allowed here.</p>
   *
   * @param tetromino piece to test (must not be {@code null})
   *
   * @return {@code true} iff every absolute cell of the piece is within bounds and on an empty cell
   *
   * @throws NullPointerException if {@code tetromino} is {@code null}
   */
  public boolean fits(Tetromino tetromino) {
    for (Cell cell : tetromino.getAbsoluteShape()) {
      int x = cell.x();
      int y = cell.y();
      if (x < 0 || x >= width || y < 0 || y >= height || cells[y][x] != 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns a new matrix with the tetromino’s absolute cells written as occupied, then applies
   * line-clear compaction.
   *
   * <p>Cells corresponding to the piece are set to {@code 1}. If your encoding stores colors
   * or IDs, adapt this method accordingly.</p>
   *
   * @param tetromino the piece to lock in place (must fit; see {@link #fits(Tetromino)})
   *
   * @return a new matrix with the piece written and lines cleared
   *
   * @throws ArrayIndexOutOfBoundsException if the piece is out of bounds (callers should ensure
   *                                        {@link #fits(Tetromino)})
   */
  public int[][] placePiece(Tetromino tetromino) {
    int[][] copy = deepCopy(cells);
    for (Cell cell : tetromino.getAbsoluteShape()) {
      copy[cell.y()][cell.x()] = 1;
    }
    return clearLines(copy);
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
