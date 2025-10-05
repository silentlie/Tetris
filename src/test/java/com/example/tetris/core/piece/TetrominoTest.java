package com.example.tetris.core.piece;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.tetris.core.board.Cell;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Tetromino")
class TetrominoTest {

  @Nested
  @DisplayName("construction")
  class Construction {

    @Test
    @DisplayName("rejects nulls")
    void nulls() {
      Cell origin = new Cell(0, 0);
      Rotation rot = Rotation.UP;

      assertThrows(NullPointerException.class, () -> new Tetromino(null, origin, rot));
      assertThrows(NullPointerException.class, () -> new Tetromino(TetrominoType.T, null, rot));
      assertThrows(NullPointerException.class, () -> new Tetromino(TetrominoType.T, origin, null));
    }
  }

  @Nested
  @DisplayName("immutability")
  class Immutability {

    @Test
    @DisplayName("withPosition returns new instance and preserves type/rotation")
    void withPosition_new() {
      Tetromino t0 = new Tetromino(TetrominoType.T, new Cell(0, 0), Rotation.UP);
      Tetromino t1 = t0.withPosition(new Cell(5, 10));

      assertEquals(new Cell(5, 10), t1.position());
      assertEquals(t0.type(), t1.type());
      assertEquals(t0.rotation(), t1.rotation());
      assertNotSame(t0, t1);
    }

    @Test
    @DisplayName("withRotation returns new instance and preserves type/position")
    void withRotation_new() {
      Tetromino t0 = new Tetromino(TetrominoType.L, new Cell(2, 3), Rotation.UP);
      Tetromino t1 = t0.withRotation(Rotation.RIGHT);

      assertEquals(Rotation.RIGHT, t1.rotation());
      assertEquals(t0.type(), t1.type());
      assertEquals(t0.position(), t1.position());
      assertNotSame(t0, t1);
    }
  }

  @Nested
  @DisplayName("geometry")
  class Geometry {

    @Test
    @DisplayName("absolute shape matches type+rotation+origin (O @ UP)")
    void absoluteShape() {
      Tetromino t = new Tetromino(TetrominoType.O, new Cell(4, 1), Rotation.UP);
      List<Cell> shape = t.getAbsoluteShape();

      assertEquals(4, shape.size());
      assertTrue(shape.contains(new Cell(4, 1)));
      assertTrue(shape.contains(new Cell(5, 1)));
      assertTrue(shape.contains(new Cell(4, 2)));
      assertTrue(shape.contains(new Cell(5, 2)));
    }

    @Test
    @DisplayName("absolute bound covers all cells")
    void absoluteBound_covers() {
      Tetromino t = new Tetromino(TetrominoType.I, new Cell(5, 0), Rotation.UP);

      Bound bound = t.getAbsoluteBound();
      List<Cell> cells = t.getAbsoluteShape();

      int minX = cells.stream().mapToInt(Cell::x).min().orElseThrow();
      int maxX = cells.stream().mapToInt(Cell::x).max().orElseThrow();
      int minY = cells.stream().mapToInt(Cell::y).min().orElseThrow();
      int maxY = cells.stream().mapToInt(Cell::y).max().orElseThrow();

      assertEquals(minX, bound.minX());
      assertEquals(maxX, bound.maxX());
      assertEquals(minY, bound.minY());
      assertEquals(maxY, bound.maxY());
    }

    @Test
    @DisplayName("absolute shape (I @ RIGHT) forms a 4×1 vertical line at origin+offset")
    void i_right_shape() {
      Tetromino t = new Tetromino(TetrominoType.I, new Cell(5, 5), Rotation.RIGHT);
      var expected = Set.of(new Cell(5, 3), new Cell(5, 4), new Cell(5, 5),
          new Cell(5, 6));
      assertEquals(expected, new HashSet<>(t.getAbsoluteShape()));
    }
  }

  @Nested
  @DisplayName("equality & hashing")
  class EqualityHashing {

    @Test
    @DisplayName("based on type, position, and rotation")
    void equals_hash() {
      Tetromino t1 = new Tetromino(TetrominoType.S, new Cell(1, 1), Rotation.DOWN);
      Tetromino t2 = new Tetromino(TetrominoType.S, new Cell(1, 1), Rotation.DOWN);
      Tetromino t3 = new Tetromino(TetrominoType.S, new Cell(2, 1), Rotation.DOWN);

      assertEquals(t1, t2);
      assertEquals(t1.hashCode(), t2.hashCode());
      assertNotEquals(t1, t3);
    }
  }
}
