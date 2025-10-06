package com.example.tetris.core.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.tetris.ai.BoardSimulator;
import com.example.tetris.core.board.Cell;
import com.example.tetris.core.piece.Rotation;
import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.piece.TetrominoType;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
  @DisplayName("equality & hashing")
  class EqualityTests {

    @Test
    @DisplayName("structural equality on dimensions, cells, tetromino, next")
    void structural() {
      int[][] a = zeros(3, 3);
      a[1][1] = 1;

      PureGame p1 = new PureGame(3, 3, a, oAt(0, 0), TetrominoType.T);
      PureGame p2 = new PureGame(3, 3, BoardSimulator.deepCopy(a), oAt(0, 0), TetrominoType.T);

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
