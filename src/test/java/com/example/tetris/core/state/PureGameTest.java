package com.example.tetris.core.state;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.tetris.core.board.Cell;
import com.example.tetris.core.piece.Rotation;
import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.piece.TetrominoType;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("PureGame")
class PureGameTest {

  private static int[][] zeros(int h, int w) {
    int[][] m = new int[h][w];
    for (int y = 0; y < h; y++) {
      Arrays.fill(m[y], 0);
    }
    return m;
  }

  private static int sum(int[][] m) {
    int s = 0;
    for (int[] row : m) {
      for (int v : row) {
        s += v;
      }
    }
    return s;
  }

  private static Tetromino oAt(int x, int y) {
    return new Tetromino(TetrominoType.O, new Cell(x, y), Rotation.UP);
  }

  @Nested
  @DisplayName("deepCopy(int[][])")
  class DeepCopyTests {

    @Test
    @DisplayName("produces equal contents but different row arrays")
    void independence() {
      int[][] original = {{1, 0, 0}, {0, 2, 0}};
      int[][] copy = PureGame.deepCopy(original);

      assertArrayEquals(original, copy);
      assertNotSame(original, copy);
      assertNotSame(original[0], copy[0]);
      assertNotSame(original[1], copy[1]);

      original[0][0] = 9;
      assertEquals(9, original[0][0]);
      assertEquals(1, copy[0][0]);
    }
  }

  @Nested
  @DisplayName("clearLines(int[][])")
  class ClearLinesTests {

    @Test
    @DisplayName("clears full rows and compacts non-full rows downward")
    void clears_and_compacts() {
      int[][] m = {
          new int[]{0, 0, 1, 0},
          new int[]{1, 1, 1, 1},
          new int[]{0, 1, 0, 0},
          new int[]{1, 1, 1, 1},
          new int[]{0, 0, 0, 1}
      };
      int[][] out = PureGame.clearLines(m);

      assertArrayEquals(new int[]{0, 0, 0, 0}, out[0]);
      assertArrayEquals(new int[]{0, 0, 0, 0}, out[1]);
      assertArrayEquals(new int[]{0, 0, 1, 0}, out[2]);
      assertArrayEquals(new int[]{0, 1, 0, 0}, out[3]);
      assertArrayEquals(new int[]{0, 0, 0, 1}, out[4]);
    }
  }

  @Nested
  @DisplayName("currentShape()/nextShape()")
  class ShapeViewsTests {

    @Test
    @DisplayName("null tetromino/next → 0x0 matrices")
    void nulls_produce_empty() {
      PureGame pg = new PureGame(10, 20, zeros(20, 10), null, null);
      assertEquals(0, pg.currentShape().length);
      assertEquals(0, pg.nextShape().length);
    }

    @Test
    @DisplayName("O piece shapes are 2x2 with four 1s (regardless of rotation for O)")
    void o_piece_shapes() {
      Tetromino t = oAt(4, 0);
      PureGame pg = new PureGame(10, 20, zeros(20, 10), t, TetrominoType.O);

      int[][] cur = pg.currentShape();
      int[][] nxt = pg.nextShape();

      assertTrue(cur.length > 0 && cur[0].length > 0);
      assertEquals(4, sum(cur));

      assertTrue(nxt.length > 0 && nxt[0].length > 0);
      assertEquals(4, sum(nxt));
    }
  }

  @Nested
  @DisplayName("fits(Tetromino)")
  class FitsTests {

    @ParameterizedTest(name = "out of bounds: x={0}, y={1}")
    @DisplayName("returns false for any cell out of bounds (including negative)")
    @CsvSource({"-1,0", "0,-1", "10,0", "0,20"})
    void oob(int x, int y) {
      PureGame pg = new PureGame(10, 20, zeros(20, 10), null, null);
      Tetromino t = oAt(x, y);
      assertFalse(pg.fits(t));
    }

    @Test
    @DisplayName("returns false on collision with non-zero cell")
    void collision() {
      int[][] cells = zeros(4, 4);
      cells[1][1] = 9; // occupied
      PureGame pg = new PureGame(4, 4, cells, null, null);

      Tetromino t = oAt(1, 1);
      assertFalse(pg.fits(t));
    }

    @Test
    @DisplayName("returns true when piece fully in bounds on empty cells")
    void fits_on_empty() {
      PureGame pg = new PureGame(10, 20, zeros(20, 10), null, null);
      assertTrue(pg.fits(oAt(4, 0)));
    }
  }

  @Nested
  @DisplayName("placePiece(Tetromino)")
  class PlacePieceTests {

    @Test
    @DisplayName("writes 1s for piece cells and then clears full lines")
    void writes_then_clears() {
      int[][] m = zeros(4, 4);
      m[3][1] = 1;
      m[3][2] = 1;
      m[3][3] = 1;

      PureGame pg = new PureGame(4, 4, m, null, null);

      int[][] after = pg.placePiece(oAt(0, 2));
      assertEquals(0, after[0][0]);
      assertEquals(1, after[3][0]);
      assertEquals(1, after[3][1]);
      assertEquals(0, after[3][2]);
      assertEquals(0, after[3][3]);
    }
  }

  @Nested
  @DisplayName("hardDrop(Tetromino)")
  class HardDropTests {

    @Test
    @DisplayName("null tetromino → deep copy (no-op)")
    void null_is_noop() {
      int[][] m = {{0, 0}, {1, 0}};
      PureGame pg = new PureGame(2, 2, m, null, null);
      int[][] out = pg.hardDrop(null);

      assertArrayEquals(m, out);
      assertNotSame(m, out);
      assertNotSame(m[0], out[0]);
    }

    @Test
    @DisplayName("non-fitting tetromino at start → deep copy (no-op)")
    void non_fitting_is_noop() {
      int[][] m = zeros(4, 4);
      m[0][0] = 1;
      PureGame pg = new PureGame(4, 4, m, null, null);

      int[][] out = pg.hardDrop(oAt(0, 0));
      assertArrayEquals(m, out);
    }

    @Test
    @DisplayName("drops piece until just before collision/floor, then locks and clears lines")
    void drops_and_locks() {
      int[][] m = zeros(6, 4);
      m[5][2] = 1;
      m[5][3] = 1;
      PureGame pg = new PureGame(4, 6, m, null, null);

      Tetromino start = oAt(0, 0);
      int[][] after = pg.hardDrop(start);

      assertEquals(1, after[5][0]);
      assertEquals(1, after[5][1]);
      assertEquals(0, after[5][2]);
      assertEquals(0, after[5][3]);
    }
  }

  @Nested
  @DisplayName("equality & hashing")
  class EqualityTests {

    @Test
    @DisplayName("structural equality on dimensions, cells, tetromino, next")
    void structural() {
      int[][] a = zeros(3, 3);
      a[1][1] = 1;

      PureGame p1 = new PureGame(3, 3, a, oAt(0, 0), TetrominoType.T);
      PureGame p2 = new PureGame(3, 3, PureGame.deepCopy(a), oAt(0, 0), TetrominoType.T);

      assertEquals(p1, p2);
      assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    @DisplayName("any differing component breaks equality")
    void inequality() {
      int[][] a = zeros(2, 2);
      int[][] b = zeros(2, 2);
      b[1][1] = 1;

      PureGame base = new PureGame(2, 2, a, null, null);

      assertNotEquals(base, new PureGame(3, 2, a, null, null), "different width");
      assertNotEquals(base, new PureGame(2, 3, a, null, null), "different height");
      assertNotEquals(base, new PureGame(2, 2, b, null, null), "different cells");
      assertNotEquals(base, new PureGame(2, 2, a, oAt(0, 0), null), "different tetromino");
      assertNotEquals(base, new PureGame(2, 2, a, null, TetrominoType.I), "different next");
    }
  }

  @Nested
  @DisplayName("toString()")
  class ToStringTests {

    @Test
    @DisplayName("contains key fields and derived shapes without throwing")
    void smoke() {
      PureGame pg = new PureGame(2, 2, zeros(2, 2), oAt(0, 0), TetrominoType.L);
      String s = pg.toString();
      assertTrue(s.contains("width=2"));
      assertTrue(s.contains("height=2"));
      assertTrue(s.contains("cells="));
      assertTrue(s.contains("currentShape="));
      assertTrue(s.contains("nextShape="));
    }
  }
}
