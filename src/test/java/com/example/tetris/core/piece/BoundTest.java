package com.example.tetris.core.piece;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Bound")
class BoundTest {

  @Nested
  @DisplayName("width/height (inclusive)")
  class Dimensions {

    @ParameterizedTest(name = "({0},{1})..({2},{3}) -> width={4}, height={5}")
    @DisplayName("computes inclusive dimensions")
    @CsvSource({
        "2,5, 4,8, 3,4",
        "0,0, 0,0, 1,1",
        "-3,-2, -1,2, 3,5",
        "5,10, 9,10, 5,1",
    })
    void dims(int minX, int minY, int maxX, int maxY, int w, int h) {
      Bound b = new Bound(minX, minY, maxX, maxY);
      assertEquals(w, b.width());
      assertEquals(h, b.height());
    }

    @Test
    @DisplayName("guards off-by-one (max - min + 1)")
    void guard() {
      Bound b = new Bound(2, 5, 4, 8);
      assertEquals(4 - 2 + 1, b.width());
      assertEquals(8 - 5 + 1, b.height());
    }
  }

  @Nested
  @DisplayName("translate")
  class Translation {

    @Test
    @DisplayName("moves all edges by (dx, dy)")
    void moves() {
      Bound b = new Bound(2, 5, 4, 8);
      Bound moved = b.translate(-1, 2);
      assertEquals(new Bound(1, 7, 3, 10), moved);
    }

    @Test
    @DisplayName("preserves dimensions")
    void preserves() {
      Bound b = new Bound(-3, -2, 1, 5);
      Bound moved = b.translate(100, -100);
      assertEquals(b.width(), moved.width());
      assertEquals(b.height(), moved.height());
    }

    @Test
    @DisplayName("is immutable")
    void immutable() {
      Bound b = new Bound(0, 0, 2, 2);
      b.translate(1, 1);
      assertEquals(new Bound(0, 0, 2, 2), b);
    }

    @Test
    @DisplayName("translation is reversible with inverse deltas")
    void reversible() {
      Bound b = new Bound(-10, -5, 3, 7);
      Bound t = b.translate(4, -2).translate(-4, 2);
      assertEquals(b, t);
    }
  }

  @Nested
  @DisplayName("record semantics")
  class RecordSemantics {

    @Test
    @DisplayName("equals/hashCode by components")
    void equals_hash() {
      assertEquals(new Bound(1, 2, 3, 4), new Bound(1, 2, 3, 4));
      assertNotEquals(new Bound(1, 2, 3, 4), new Bound(1, 2, 3, 5));
    }

    @Test
    @DisplayName("toString is stable")
    void format() {
      Bound b = new Bound(2, 5, 4, 8);
      assertEquals("Bound[minX=2, minY=5, maxX=4, maxY=8]", b.toString());
    }

    @Test
    @DisplayName("accessors return components")
    void accessors() {
      Bound b = new Bound(-1, -2, 9, 10);
      assertAll(
          () -> assertEquals(-1, b.minX()),
          () -> assertEquals(-2, b.minY()),
          () -> assertEquals(9, b.maxX()),
          () -> assertEquals(10, b.maxY())
      );
    }
  }

  @Nested
  @DisplayName("edge cases")
  class EdgeCases {

    @Test
    @DisplayName("degenerate single-point bound is valid (1×1)")
    void single_point() {
      Bound b = new Bound(7, 7, 7, 7);
      assertEquals(1, b.width());
      assertEquals(1, b.height());
      assertEquals("Bound[minX=7, minY=7, maxX=7, maxY=7]", b.toString());
    }

    @Test
    @DisplayName("constructor enforces min/max invariants")
    void invariants() {
      assertThrows(IllegalArgumentException.class, () -> new Bound(5, 5, 3, 5)); // maxX < minX
      assertThrows(IllegalArgumentException.class, () -> new Bound(5, 5, 7, 4)); // maxY < minY
    }

    @Test
    @DisplayName("near-INT_MAX dimensions compute without overflow")
    void near_max() {
      Bound b = new Bound(
          Integer.MAX_VALUE - 2,
          Integer.MAX_VALUE - 2,
          Integer.MAX_VALUE,
          Integer.MAX_VALUE
      );
      assertEquals(3, b.width());
      assertEquals(3, b.height());
    }

    @Test
    @DisplayName("translate overflows throw ArithmeticException")
    void translate_overflow() {
      // X overflow
      Bound bx = new Bound(Integer.MAX_VALUE - 1, 0, Integer.MAX_VALUE, 0);
      assertThrows(ArithmeticException.class, () -> bx.translate(1, 0));
      assertThrows(ArithmeticException.class, () -> bx.translate(2, 0));

      // Y underflow
      Bound by = new Bound(0, Integer.MIN_VALUE, 0, Integer.MIN_VALUE);
      assertThrows(ArithmeticException.class, () -> by.translate(0, -1));
    }

    @Test
    @DisplayName("width/height overflow throw ArithmeticException")
    void dims_overflow() {
      Bound wide = new Bound(Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 0);
      assertThrows(ArithmeticException.class, wide::width);
      Bound tall = new Bound(0, Integer.MIN_VALUE, 0, Integer.MAX_VALUE);
      assertThrows(ArithmeticException.class, tall::height);
    }

    @Test
    @DisplayName("translate by zero is a semantic no-op")
    void translate_zero() {
      Bound b = new Bound(-10, -20, -5, -1);
      Bound t = b.translate(0, 0);
      assertEquals(b, t);
    }
  }
}
