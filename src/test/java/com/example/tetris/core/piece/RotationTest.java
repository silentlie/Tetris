package com.example.tetris.core.piece;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.tetris.core.board.Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Rotation enum")
class RotationTest {

  @Test
  @DisplayName("ordering is clockwise and size is 4")
  void ordering() {
    Rotation[] vals = Rotation.values();
    assertEquals(4, vals.length);
    assertArrayEquals(new Rotation[]{Rotation.UP, Rotation.RIGHT, Rotation.DOWN, Rotation.LEFT},
        vals);
  }

  @Test
  @DisplayName("rotateClockwise() cycles UP→RIGHT→DOWN→LEFT→UP")
  void cw() {
    assertEquals(Rotation.RIGHT, Rotation.UP.rotateClockwise());
    assertEquals(Rotation.DOWN, Rotation.RIGHT.rotateClockwise());
    assertEquals(Rotation.LEFT, Rotation.DOWN.rotateClockwise());
    assertEquals(Rotation.UP, Rotation.LEFT.rotateClockwise());
  }

  @Test
  @DisplayName("rotateCounterClockwise() cycles UP→LEFT→DOWN→RIGHT→UP")
  void ccw() {
    assertEquals(Rotation.LEFT, Rotation.UP.rotateCounterClockwise());
    assertEquals(Rotation.UP, Rotation.RIGHT.rotateCounterClockwise());
    assertEquals(Rotation.RIGHT, Rotation.DOWN.rotateCounterClockwise());
    assertEquals(Rotation.DOWN, Rotation.LEFT.rotateCounterClockwise());
  }

  @Test
  @DisplayName("cw and ccw are inverses")
  void inverses() {
    for (Rotation r : Rotation.values()) {
      assertEquals(r, r.rotateClockwise().rotateCounterClockwise());
      assertEquals(r, r.rotateCounterClockwise().rotateClockwise());
    }
  }

  @Test
  @DisplayName("four clockwise steps return to start")
  void fourSteps() {
    for (Rotation r : Rotation.values()) {
      Rotation s = r.rotateClockwise().rotateClockwise()
          .rotateClockwise().rotateClockwise();
      assertSame(r, s);
    }
  }

  @Test
  @DisplayName("apply transforms coordinates correctly")
  void apply() {
    Cell v = new Cell(1, 2);
    assertEquals(new Cell(1, 2), Rotation.UP.apply(v));
    assertEquals(new Cell(2, -1), Rotation.RIGHT.apply(v));
    assertEquals(new Cell(-1, -2), Rotation.DOWN.apply(v));
    assertEquals(new Cell(-2, 1), Rotation.LEFT.apply(v));
  }

  @Test
  @DisplayName("apply is linear over addition (r(a+b) = r(a) + r(b))")
  void linearity() {
    Cell a = new Cell(3, -2);
    Cell b = new Cell(-5, 7);
    for (Rotation r : Rotation.values()) {
      Cell left = r.apply(a.add(b));
      Cell right = r.apply(a).add(r.apply(b));
      assertEquals(right, left, () -> "Linearity failed for " + r);
    }
  }

  @Test
  @DisplayName("apply(null) throws NullPointerException")
  void nullInput() {
    assertThrows(NullPointerException.class, () -> Rotation.UP.apply(null));
  }

  @Test
  @DisplayName("UP.apply returns same instance (identity)")
  void identityCaching() {
    Cell c = new Cell(9, -4);
    assertSame(c, Rotation.UP.apply(c));
  }
}
