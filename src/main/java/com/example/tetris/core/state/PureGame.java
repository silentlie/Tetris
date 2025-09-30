package com.example.tetris.core.state;

import com.example.tetris.core.board.Cell;
import com.example.tetris.core.piece.Rotation;
import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.piece.TetrominoType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, minimal snapshot of a Tetris board suitable for AI evaluation, serialization, and
 * deterministic testing.
 *
 * <p>This {@code record} captures:
 * <ul>
 *   <li>{@code width}/{@code height}: board dimensions in cells</li>
 *   <li>{@code cells}: a 2D raster of occupied cells where {@code 0} = empty, non-zero = filled
 *   </li>
 *   <li>{@code tetromino}: the currently active falling piece (maybe {@code null})</li>
 *   <li>{@code next}: the next piece in the queue (maybe {@code null})</li>
 * </ul>
 *
 * <p>While the record itself is immutable, the {@code cells} array is a nested array
 * and thus mutable by reference; methods that need to mutate board state (line clear,
 * placement) return <em>new</em> arrays created via {@link #deepCopy(int[][])} to preserve
 * snapshot semantics.
 *
 * <h2>Serialization</h2>
 * The auxiliary “shape” views of the current and next piece are emitted for JSON under
 * {@code currentShape} and {@code nextShape} via {@link #currentShape()} and
 * {@link #nextShape()}. These are computed matrices (not part of equality).
 *
 * <h2>Equality</h2>
 * Equality compares dimensions, a deep element-wise comparison of {@code cells},
 * and structural equality of {@code tetromino} and {@code next}. See
 * {@link #equals(Object)} for details.
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
   * (classic Tetris "line clear").
   *
   * <p>Algorithm:
   * <ol>
   *   <li>Scan {@code original} top-to-bottom, detect full rows (no zeros).</li>
   *   <li>Copy only non-full rows into a new buffer from the bottom up.</li>
   *   <li>Fill any remaining top rows with empty rows (all zeros).</li>
   * </ol>
   *
   * <p>Time complexity is {@code O(rows * cols)}; space complexity is {@code O(rows * cols)} for
   * the copy.</p>
   *
   * @param original source board matrix (not modified)
   *
   * @return a new matrix with full lines cleared and gapless compaction applied
   *
   * @throws NullPointerException if {@code original} or its rows are {@code null}
   * @implNote This method assumes a rectangular matrix (constant row length).
   */
  public int[][] clearLines(int[][] original) {
    int[][] copy = new int[original.length][];
    int write = original.length - 1;

    for (int[] row : original) {
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
   * 0/1 matrix with origin at the piece's pivot.
   *
   * <p>Exposed for JSON as {@code "currentShape"}.</p>
   *
   * @return a new 2D matrix describing the current piece shape, or {@code 0x0} if
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
   * @return a new 2D matrix describing the next piece shape, or {@code 0x0} if {@code next == null}
   */
  @JsonProperty("nextShape")
  public int[][] nextShape() {
    if (next == null) {
      return new int[0][0];
    }
    return next.relativeDerived(Rotation.UP).toShapeMatrix();
  }

  /**
   * Simulates a hard drop of the given {@code tetromino} and returns a new board with the piece
   * locked and any resulting line clears applied.
   *
   * <p>Algorithm: repeatedly attempt to move the piece one row down while the next position
   * {@link #fits(Tetromino)}; when the next move no longer fits, lock at the last valid position
   * and {@link #placePiece(Tetromino) place} it (then clear full lines).</p>
   *
   * <p>If {@code tetromino} is {@code null} or does not fit at its current position (e.g., spawn
   * overlap), this method returns a deep copy of {@link #cells} (no-op).</p>
   *
   * @param tetromino the piece to drop (its current position and rotation are respected)
   *
   * @return a new matrix with the piece written and full lines cleared; or an unchanged deep copy
   *     of {@link #cells} if the piece is {@code null} or cannot be placed
   *
   * @implNote Runs in O(drop distance × cells per piece) time. The board uses value {@code 1}
   *     for filled cells; adapt {@link #placePiece(Tetromino)} if you encode colors/IDs.
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
   * @param tetromino piece to test
   *
   * @return {@code true} iff every absolute cell of the piece is within bounds and on an empty cell
   *
   * @throws NullPointerException if {@code tetromino} is {@code null}
   * @implNote Uses the piece's absolute cell coordinates; negative Y or X or exceeding
   *     width/height fails.
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
   * Returns a new matrix with the given tetromino's absolute cells written as occupied, followed by
   * line-clear compaction.
   *
   * <p>Cells corresponding to the piece are set to {@code 1}. If you encode colors or IDs
   * in the board, adapt this method accordingly.</p>
   *
   * @param tetromino the piece to lock in place
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
   * <p>Two {@code PureGame} instances are equal iff:
   * <ul>
   *   <li>{@code width} and {@code height} are equal</li>
   *   <li>{@code cells} is deeply equal via {@link Arrays#deepEquals(Object[], Object[])}</li>
   *   <li>{@code tetromino} and {@code next} are equal via {@link Objects#equals(Object, Object)}
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
   * Hash code consistent with {@link #equals(Object)}, combining dimensions, a deep hash of
   * {@code cells}, and the piece fields.
   */
  @Override
  public int hashCode() {
    int hashCells = Arrays.deepHashCode(cells);
    return Objects.hash(width, height, hashCells, tetromino, next);
  }

  @Override
  public String toString() {
    return "PureGame{"
        + "width=" + width
        + ", height=" + height
        + ", cells=" + Arrays.deepToString(cells)
        + ", currentShape=" + Arrays.deepToString(currentShape())
        + ", nextShape=" + Arrays.deepToString(nextShape())
        + '}';
  }
}
