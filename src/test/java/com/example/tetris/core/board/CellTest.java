package com.example.tetris.core.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Cell")
public class CellTest {

  @Nested
  @DisplayName("direction constants")
  class DirectionConstants {

    @Test
    @DisplayName("UP is (0, -1)")
    void up() {
      assertEquals(0, Cell.UP.x());
      assertEquals(-1, Cell.UP.y());
    }

    @Test
    @DisplayName("DOWN is (0, +1)")
    void down() {
      assertEquals(0, Cell.DOWN.x());
      assertEquals(1, Cell.DOWN.y());
    }

    @Test
    @DisplayName("LEFT is (-1, 0)")
    void left() {
      assertEquals(-1, Cell.LEFT.x());
      assertEquals(0, Cell.LEFT.y());
    }

    @Test
    @DisplayName("RIGHT is (+1, 0)")
    void right() {
      assertEquals(1, Cell.RIGHT.x());
      assertEquals(0, Cell.RIGHT.y());
    }

    @Test
    @DisplayName("direction values are immutable")
    void immutable() {
      Cell before = Cell.UP;
      Cell moved = before.add(Cell.RIGHT);
      assertEquals(0, before.x());
      assertEquals(-1, before.y());
      assertEquals(1, moved.x());
      assertEquals(-1, moved.y());
    }

    @Test
    @DisplayName("direction constants are distinct instances with correct coords")
    void directionsDistinct() {
      assertNotEquals(Cell.UP, Cell.RIGHT);
      assertNotEquals(Cell.UP, Cell.LEFT);
      assertNotEquals(Cell.UP, Cell.DOWN);
      assertNotEquals(Cell.RIGHT, Cell.LEFT);
      assertNotEquals(Cell.RIGHT, Cell.DOWN);
      assertNotEquals(Cell.DOWN, Cell.LEFT);

    }
  }

  @Nested
  @DisplayName("add(int dx, int dy)")
  class AddDeltas {

    @ParameterizedTest(name = "({0},{1}) + ({2},{3}) → ({4},{5})")
    @DisplayName("adds deltas")
    @CsvSource({
        "0,0, 0,0, 0,0",
        "3,4, 1,-2, 4,2",
        "-5,10, 7,-3, 2,7",
        "1,-1, -1,1, 0,0"
    })
    void adds(int x, int y, int dx, int dy, int expX, int expY) {
      Cell base = new Cell(x, y);
      Cell result = base.add(dx, dy);
      assertEquals(expX, result.x());
      assertEquals(expY, result.y());
    }

    @Test
    @DisplayName("does not mutate receiver")
    void noMutation() {
      Cell base = new Cell(9, -4);
      base.add(1, 1);
      assertEquals(9, base.x());
      assertEquals(-4, base.y());
    }

    @Test
    @DisplayName("add respects identity and composition")
    void addAlgebra() {
      Cell p = new Cell(5, -2);
      Cell d1 = new Cell(3, 4);
      Cell d2 = new Cell(-1, 7);

      assertEquals(p, p.add(0, 0));
      Cell left = p.add(d1).add(d2);
      Cell right = p.add(d1.add(d2));
      assertEquals(left, right);
    }

    @Test
    @DisplayName("add throws on int overflow")
    void addOverflow() {
      Cell c = new Cell(Integer.MAX_VALUE, Integer.MIN_VALUE);
      assertThrows(ArithmeticException.class, () -> c.add(1, 0));
      assertThrows(ArithmeticException.class, () -> c.add(new Cell(0, -1)));
    }
  }

  @Nested
  @DisplayName("value semantics (record)")
  class ValueSemantics {

    @Test
    @DisplayName("equal when same coordinates")
    void equal() {
      Cell a = new Cell(2, 3);
      Cell b = new Cell(2, 3);
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("not equal when different coordinates")
    void notEqual() {
      assertNotEquals(new Cell(2, 3), new Cell(3, 2));
      assertNotEquals(new Cell(2, 3), new Cell(2, 4));
      assertNotEquals(new Cell(2, 3), new Cell(1, 3));
    }

    @Test
    @DisplayName("toString contains coordinates")
    void format() {
      String s = new Cell(-1, 7).toString();
      assertTrue(s.contains("x=-1") && s.contains("y=7"));
    }

    @Test
    @DisplayName("equal cells share hashCode")
    void hashConsistency() {
      assertEquals(new Cell(2, 3).hashCode(), new Cell(2, 3).hashCode());
    }
  }
}
