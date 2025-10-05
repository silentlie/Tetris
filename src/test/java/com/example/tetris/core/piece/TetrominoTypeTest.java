package com.example.tetris.core.piece;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.tetris.core.board.Cell;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TetrominoType")
class TetrominoTypeTest {

  @Nested
  @DisplayName("color mapping")
  class Colors {

    @Test
    @DisplayName("colors match Tetris convention")
    void colors() {
      assertEquals(Color.CYAN, TetrominoType.I.getColor());
      assertEquals(Color.YELLOW, TetrominoType.O.getColor());
      assertEquals(Color.MAGENTA, TetrominoType.T.getColor());
      assertEquals(Color.GREEN, TetrominoType.S.getColor());
      assertEquals(Color.RED, TetrominoType.Z.getColor());
      assertEquals(Color.BLUE, TetrominoType.J.getColor());
      assertEquals(Color.ORANGE, TetrominoType.L.getColor());
    }
  }

  @Nested
  @DisplayName("relative shape & bounds")
  class RelativeShapes {

    @Test
    @DisplayName("O at UP: 2×2 square, bound [0..1]×[0..1]")
    void O_up() {
      TetrominoType t = TetrominoType.O;
      Rotation r = Rotation.UP;

      List<Cell> rel = t.relativeShape(r);
      assertEquals(
          Set.of(new Cell(0, 0), new Cell(1, 0), new Cell(0, 1), new Cell(1, 1)),
          Set.copyOf(rel),
          "O shape must be 2x2 anchored at origin"
      );

      Bound b = t.relativeBound(r);
      assertEquals(new Bound(0, 0, 1, 1), b);
      assertEquals(2, b.width());
      assertEquals(2, b.height());
    }

    @Test
    @DisplayName("I at UP and RIGHT: line flips orientation and bound swaps")
    void I_up_right() {
      TetrominoType t = TetrominoType.I;

      var rel = t.relativeShape(Rotation.UP);
      assertEquals(
          Set.of(new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(2, 0)),
          Set.copyOf(rel)
      );
      var b = t.relativeBound(Rotation.UP);
      assertEquals(new Bound(-1, 0, 2, 0), b);
      assertEquals(4, b.width());
      assertEquals(1, b.height());

      rel = t.relativeShape(Rotation.RIGHT);
      assertEquals(
          Set.of(new Cell(0, -2), new Cell(0, -1), new Cell(0, 0), new Cell(0, 1)),
          Set.copyOf(rel)
      );
      b = t.relativeBound(Rotation.RIGHT);
      assertEquals(new Bound(0, -2, 0, 1), b);
      assertEquals(1, b.width());
      assertEquals(4, b.height());
    }

    @Test
    @DisplayName("T at UP: center at origin with stem down")
    void T_up() {
      var rel = TetrominoType.T.relativeShape(Rotation.UP);
      assertEquals(
          Set.of(new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(0, 1)),
          Set.copyOf(rel)
      );
      var b = TetrominoType.T.relativeBound(Rotation.UP);
      assertEquals(new Bound(-1, 0, 1, 1), b);
    }

    @Test
    @DisplayName("shape lists are unmodifiable")
    void unmodifiable() {
      var rel = TetrominoType.S.relativeShape(Rotation.LEFT);
      assertThrows(UnsupportedOperationException.class, () -> rel.add(new Cell(99, 99)));
    }

    @Test
    @DisplayName("S bounds across rotations are correct")
    void s_bounds() {
      var expected = java.util.Map.of(
          Rotation.UP, new Bound(-1, 0, 1, 1),
          Rotation.RIGHT, new Bound(0, -1, 1, 1),
          Rotation.DOWN, new Bound(-1, -1, 1, 0),
          Rotation.LEFT, new Bound(-1, -1, 0, 1)
      );
      for (var e : expected.entrySet()) {
        assertEquals(e.getValue(), TetrominoType.S.relativeBound(e.getKey()), "S@" + e.getKey());
      }
    }
  }

  @Nested
  @DisplayName("Derived.toShapeMatrix()")
  class Matrix {

    @Test
    @DisplayName("O at UP becomes 2×2 matrix of ones")
    void O_matrix() {
      var d = TetrominoType.O.relativeDerived(Rotation.UP);
      int[][] bits = d.toShapeMatrix();
      assertEquals(2, bits.length);
      assertEquals(2, bits[0].length);
      assertEquals(1, bits[0][0]);
      assertEquals(1, bits[0][1]);
      assertEquals(1, bits[1][0]);
      assertEquals(1, bits[1][1]);
    }

    @Test
    @DisplayName("I at RIGHT becomes 4×1 column of ones")
    void I_column() {
      var d = TetrominoType.I.relativeDerived(Rotation.RIGHT);
      int[][] bits = d.toShapeMatrix();
      assertEquals(4, bits.length);
      for (int row = 0; row < 4; row++) {
        assertEquals(1, bits[row].length);
        assertEquals(1, bits[row][0]);
      }
    }

    @Test
    @DisplayName("matrix indices align with bound min corner (ny=y-minY, nx=x-minX)")
    void index_align() {
      var d = TetrominoType.J.relativeDerived(Rotation.UP);
      Bound b = d.bound();
      int[][] bits = d.toShapeMatrix();
      for (Cell c : d.relative()) {
        int nx = c.x() - b.minX();
        int ny = c.y() - b.minY();
        assertEquals(1, bits[ny][nx], "bit not set for " + c);
      }
    }
  }

  @Nested
  @DisplayName("caching & concurrency")
  class Caching {

    @Test
    @DisplayName("same type+rotation returns same Derived instance (cached)")
    void same_instance() {
      var d1 = TetrominoType.Z.relativeDerived(Rotation.LEFT);
      var d2 = TetrominoType.Z.relativeDerived(Rotation.LEFT);
      assertSame(d1, d2);
    }

    @Test
    @DisplayName("cache works under parallel access")
    void parallel() {
      var seen = new ConcurrentLinkedQueue<TetrominoType.Derived>();
      IntStream.range(0, 1_000).parallel().forEach(i -> {
        var d = TetrominoType.L.relativeDerived(Rotation.DOWN);
        seen.add(d);
      });
      var first = TetrominoType.L.relativeDerived(Rotation.DOWN);
      assertTrue(seen.stream().allMatch(d -> d == first));
    }
  }

  @Nested
  @DisplayName("rotation consistency")
  class RotationConsistency {

    @Test
    @DisplayName("relativeShape equals applying Rotation to canonical shape")
    void matches() {
      for (TetrominoType t : TetrominoType.values()) {
        for (Rotation r : Rotation.values()) {
          var expected = tRelativeCanonical(t).stream().map(r::apply)
              .collect(Collectors.toSet());
          var actual = Set.copyOf(t.relativeShape(r));
          assertEquals(expected, actual, () -> t + "@" + r);
        }
      }
    }

    private List<Cell> tRelativeCanonical(TetrominoType t) {
      return switch (t) {
        case I -> List.of(new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(2, 0));
        case O -> List.of(new Cell(0, 0), new Cell(1, 0), new Cell(0, 1), new Cell(1, 1));
        case T -> List.of(new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(0, 1));
        case S -> List.of(new Cell(0, 0), new Cell(1, 0), new Cell(-1, 1), new Cell(0, 1));
        case Z -> List.of(new Cell(-1, 0), new Cell(0, 0), new Cell(0, 1), new Cell(1, 1));
        case J -> List.of(new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(-1, 1));
        case L -> List.of(new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(1, 1));
      };
    }
  }
}
