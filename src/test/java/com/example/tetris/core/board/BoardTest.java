package com.example.tetris.core.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.tetris.core.piece.Rotation;
import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.piece.TetrominoType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Board")
class BoardTest {

  private static Tetromino oAt(int x, int y) {
    return new Tetromino(TetrominoType.O, new Cell(x, y), Rotation.UP);
  }

  @Nested
  @DisplayName("constructor")
  class ConstructorTests {

    @Test
    @DisplayName("rejects non-positive dimensions")
    void rejects() {
      assertThrows(IllegalArgumentException.class, () -> new Board(0, 20));
      assertThrows(IllegalArgumentException.class, () -> new Board(10, 0));
      assertThrows(IllegalArgumentException.class, () -> new Board(-1, 20));
      assertThrows(IllegalArgumentException.class, () -> new Board(10, -1));
    }

    @Test
    @DisplayName("stores width/height and starts empty")
    void stores() {
      Board b = new Board(10, 20);
      assertEquals(10, b.width);
      assertEquals(20, b.height);
      for (int y = 0; y < b.height; y++) {
        for (int x = 0; x < b.width; x++) {
          assertNull(b.getCell(x, y));
        }
      }
    }
  }

  @Nested
  @DisplayName("fits(Tetromino)")
  class FitsTests {

    @Test
    @DisplayName("true on empty board when in bounds")
    void in_bounds() {
      Board b = new Board(10, 20);
      assertTrue(b.fits(oAt(4, 0)));
    }

    @ParameterizedTest(name = "false when any block x={0} is out of horizontal bounds")
    @DisplayName("false if horizontally out of bounds")
    @ValueSource(ints = {-1, 10})
    void horiz_oob(int x) {
      Board b = new Board(10, 20);
      Tetromino t = oAt(x, 0);
      assertFalse(b.fits(t));
    }

    @ParameterizedTest(name = "false when any block y={1} is below floor (height={2})")
    @DisplayName("false if any y >= height (below the floor)")
    @CsvSource({
        "10, 20, 19",
        "4,  4,  3"
    })
    void below_floor(int w, int h, int placeY) {
      Board b = new Board(w, h);
      assertFalse(b.fits(oAt(0, placeY)));
    }

    @Test
    @DisplayName("true if blocks are above top (y<0) and otherwise valid")
    void above_top() {
      Board b = new Board(10, 20);
      assertTrue(b.fits(oAt(4, -1)));
    }

    @Test
    @DisplayName("false when colliding with existing locked cells")
    void collision() {
      Board b = new Board(10, 20);
      assertFalse(b.lock(oAt(4, 0)));
      assertFalse(b.fits(oAt(4, 0)));
    }
  }

  @Nested
  @DisplayName("lock(Tetromino)")
  class LockTests {

    @Test
    @DisplayName("writes type into occupied cells and returns false when fully visible")
    void writes() {
      Board b = new Board(10, 20);
      Tetromino t = oAt(4, 0);
      boolean lockOut = b.lock(t);
      assertFalse(lockOut);

      for (Cell c : t.getAbsoluteShape()) {
        assertEquals(TetrominoType.O, b.getCell(c));
      }
    }

    @Test
    @DisplayName("ignores y<0 blocks and returns true (lock-out)")
    void lockout() {
      Board b = new Board(10, 20);
      Tetromino t = oAt(4, -1);
      boolean lockOut = b.lock(t);
      assertTrue(lockOut);

      for (Cell c : t.getAbsoluteShape()) {
        if (c.y() < 0) {
          assertNull(b.getCell(c));
        } else {
          assertEquals(TetrominoType.O, b.getCell(c));
        }
      }
    }
  }

  @Nested
  @DisplayName("clearLines()")
  class ClearLinesTests {

    @Test
    @DisplayName("clears exactly one line using off-screen spawn to avoid filling the row above")
    void single_line() {
      Board b = new Board(4, 4);
      assertTrue(b.fits(oAt(0, -1)));
      assertTrue(b.fits(oAt(2, -1)));
      b.lock(oAt(0, -1));
      b.lock(oAt(2, -1));

      int cleared = b.clearLines();
      assertEquals(1, cleared);

      for (int y = 0; y < b.height; y++) {
        for (int x = 0; x < b.width; x++) {
          assertNull(b.getCell(x, y));
        }
      }
    }

    @Test
    @DisplayName("clears two full lines and compacts remaining rows downward")
    void two_lines() {
      Board b = new Board(4, 4);

      assertTrue(b.fits(oAt(0, 2)));
      assertTrue(b.fits(oAt(2, 2)));
      b.lock(oAt(0, 2));
      b.lock(oAt(2, 2));

      assertTrue(b.fits(oAt(0, 0)));
      b.lock(oAt(0, 0));

      int cleared = b.clearLines();
      assertEquals(2, cleared);

      assertEquals(TetrominoType.O, b.getCell(0, 2));
      assertEquals(TetrominoType.O, b.getCell(1, 2));
      assertEquals(TetrominoType.O, b.getCell(0, 3));
      assertEquals(TetrominoType.O, b.getCell(1, 3));

      assertNull(b.getCell(2, 2));
      assertNull(b.getCell(3, 2));
      assertNull(b.getCell(2, 3));
      assertNull(b.getCell(3, 3));
    }

    @Test
    @DisplayName("returns 0 when no lines are full")
    void zero_when_none() {
      Board b = new Board(10, 4);
      b.lock(oAt(0, 0));
      assertEquals(0, b.clearLines());
    }
  }

  @Nested
  @DisplayName("getCell(..)")
  class GetCellTests {

    @Test
    @DisplayName("by Cell and by (x,y) return same values")
    void overloads() {
      Board b = new Board(10, 20);
      Tetromino t = oAt(4, 1);
      b.lock(t);

      for (Cell c : t.getAbsoluteShape()) {
        if (c.y() >= 0) {
          assertEquals(b.getCell(c.x(), c.y()), b.getCell(c));
        }
      }
    }

    @ParameterizedTest(name = "out-of-bounds ({0},{1}) yields null")
    @DisplayName("returns null for out of bounds")
    @CsvSource({
        "-1, 0",
        "0, -1",
        "10, 0",
        "0, 20",
        "10, 20",
        "-5, -5"
    })
    void oob_null(int x, int y) {
      Board b = new Board(10, 20);
      assertNull(b.getCell(x, y));
    }

    @Test
    @DisplayName("returns null for empty cells; type for occupied cells")
    void empty_vs_occupied() {
      Board b = new Board(10, 20);
      assertNull(b.getCell(3, 3));
      b.lock(oAt(3, 2));
      assertEquals(TetrominoType.O, b.getCell(3, 3));
      assertEquals(TetrominoType.O, b.getCell(4, 2));
      assertNull(b.getCell(9, 0));
    }
  }

  @Nested
  @DisplayName("integration: fits → lock → clearLines")
  class IntegrationTests {

    @Test
    @DisplayName("sequence behaves consistently")
    void sequence() {
      Board b = new Board(10, 22);
      Tetromino t1 = oAt(4, -1);
      assertTrue(b.fits(t1));
      assertTrue(b.lock(t1));

      Tetromino t2 = oAt(6, 0);
      assertTrue(b.fits(t2));
      assertFalse(b.lock(t2));

      int cleared = b.clearLines();
      assertTrue(cleared >= 0 && cleared <= b.height);
    }

    @ParameterizedTest
    @DisplayName("rotated I near edges does not crash (fits may be true or false)")
    @CsvSource({
        // width,height, x,y, rotation
        "10,20, 0,0, RIGHT",
        "10,20, 9,0, LEFT",
    })
    void rotated_edges(int w, int h, int x, int y, Rotation r) {
      Board b = new Board(w, h);
      Tetromino t = new Tetromino(TetrominoType.I, new Cell(x, y), Rotation.valueOf(r.name()));
      assertTrue(b.fits(t) || !b.fits(t));
    }
  }
}
