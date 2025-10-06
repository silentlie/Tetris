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

/**
 * Unit testing for the {@link Tetromino} record.
 * Includes: movement, rotation, absolute shape checks, factory invariants,
 * construction invariants including nulls, and immutability.
 */
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


  /**
   * Moving up from (5,10) should change pivot point to (5,9) and return
   * a new instance while leaving the original unchanged.
   */
  @Test
  void moveUp() {
    Tetromino t = TestHelpers.of(TetrominoType.T, 5, 10, Rotation.UP);
    Tetromino moved = t.moveUp();
    TestHelpers.assertMovedTo(t, moved, new Cell(5,9)); // new pivot point (5,9)
    assertEquals(new Cell(5,10), t.position()); // original unchanged
  }

  @Test
  void moveDown() {
    Tetromino t = TestHelpers.of(TetrominoType.T, 5, 10, Rotation.UP);
    Tetromino moved = t.moveDown();
    TestHelpers.assertMovedTo(t, moved, new Cell(5,11));
    assertEquals(new Cell(5, 10), t.position());
  }

  @Test
  void moveLeft() {
    Tetromino t = TestHelpers.of(TetrominoType.T, 5, 10, Rotation.UP);
    Tetromino moved = t.moveLeft();
    TestHelpers.assertMovedTo(t, moved, new Cell(4, 10));
    assertEquals(new Cell(5, 10), t.position());
  }

  @Test
  void moveRight() {
    Tetromino t = TestHelpers.of(TetrominoType.T, 5, 10, Rotation.UP);
    Tetromino moved = t.moveRight();
    TestHelpers.assertMovedTo(t, moved, new Cell(6, 10));
    assertEquals(new Cell(5, 10), t.position());
  }

  /**
   * rotateClockwise() should go Up > right > down > left > up, return a new instance,
   * and preserve type/position
   */
  @Test
  void rotateClockwise() {
    Tetromino t = TestHelpers.of(TetrominoType.L, 3, 7, Rotation.UP);
    Tetromino r1 = t.rotateClockwise();
    TestHelpers.assertRotatedTo(t, r1, Rotation.RIGHT);

    // Full clockwise cycle check
    assertEquals(Rotation.DOWN, r1.rotateClockwise().rotation());
    assertEquals(Rotation.LEFT, r1.rotateClockwise().rotateClockwise().rotation());
    assertEquals(Rotation.UP, r1.rotateClockwise().rotateClockwise().rotateClockwise().rotation());
  }

  /**
   * rotateCounterClockwise() should go up > left > down > right > up, return a new instance,
   * and preserve type/position
   */
  @Test
  void rotateCounterClockwise() {
    Tetromino t = TestHelpers.of(TetrominoType.L, 3, 7, Rotation.UP);
    Tetromino r1 = t.rotateCounterClockwise();
    TestHelpers.assertRotatedTo(t, r1, Rotation.LEFT);

    // Full counter-clockwise cycle check
    assertEquals(Rotation.DOWN, r1.rotateCounterClockwise().rotation());
    assertEquals(Rotation.RIGHT, r1.rotateCounterClockwise().rotateCounterClockwise().rotation());
    assertEquals(Rotation.UP, r1.rotateCounterClockwise().rotateCounterClockwise().rotateCounterClockwise().rotation());
  }


  /**
   * withPosition should return a new instance with an updated pivot point and preserve the
   * type/rotation. The original instance must be unchanged.
   */
  @Test
  void withPosition() {
    Tetromino base = TestHelpers.of(TetrominoType.T, 3, 7, Rotation.RIGHT);
    Tetromino moved = base.withPosition(new Cell(10, 2));

    assertNotSame(base, moved, "withPosition must return a new instance");
    assertEquals(new Cell(10, 2), moved.position());
    assertEquals(base.type(), moved.type(), "type must be preserved");
    assertEquals(base.rotation(), moved.rotation(), "rotation must be preserved");

    // Original unchanged (immutability)
    assertEquals(new Cell(3, 7), base.position());
  }

  /**
   * Should return a new instance with that rotation preserving type and position. The original
   * instance must be unchanged.
   */
  @Test
  void withRotation() {
    Tetromino base = TestHelpers.of(TetrominoType.I, 0, 0, Rotation.UP);
    Tetromino r = base.withRotation(Rotation.LEFT);

    assertNotSame(base, r, "withRotation must return a new instance");
    assertEquals(Rotation.LEFT, r.rotation());
    assertEquals(base.type(), r.type(), "type must be preserved");
    assertEquals(base.position(), r.position(), "position must be preserved");

    // Original unchanged (immutability)
    assertEquals(Rotation.UP, base.rotation());
  }


  @Test
  void getAbsoluteShape() {
    // I piece at origin, UP: Shapes has offsets
    Tetromino iUp = TestHelpers.of(TetrominoType.I, 0, 0, Rotation.UP);
    Set<Cell> expectedIUp = Set.of(
        new Cell(-2, 0),
        new Cell(-1, 0),
        new Cell(0, 0),
        new Cell(1, 0)
    );

    TestHelpers.assertCellSetEquals(expectedIUp, iUp.getAbsoluteShape());

    // O piece is rotation-invariant, origin at (0,0)
    Tetromino oUp = TestHelpers.of(TetrominoType.O, 0, 0, Rotation.UP);
    Set<Cell> expectedO = Set.of(
        new Cell(0,0),
        new Cell(1, 0),
        new Cell(0,1),
        new Cell(1,1)
    );
    TestHelpers.assertCellSetEquals(expectedO, oUp.getAbsoluteShape());

    // O @ right
    Tetromino oRight = TestHelpers.of(TetrominoType.O, 0, 0, Rotation.RIGHT);
    Set<Cell> expectedORight = Set.of(
        new Cell(0,  0), new Cell(1,  0),
        new Cell(0, -1), new Cell(1, -1)
    );
    TestHelpers.assertCellSetEquals(expectedORight, oRight.getAbsoluteShape());

    // I @ right
    Tetromino iRight = TestHelpers.of(TetrominoType.I, 0, 0, Rotation.RIGHT);
    Set<Cell> expectedIRight = Set.of(
        new Cell(0, -1),
        new Cell(0, 0),
        new Cell(0,  1),
        new Cell(0,  2)

    );
    TestHelpers.assertCellSetEquals(expectedIRight, iRight.getAbsoluteShape());

  }

  /** Constructor should store type, position and rotation. */
  @Test
  void construction_storesFields() {
    TetrominoType type = TetrominoType.Z;
    Cell pos = new Cell(2, 3);
    Rotation r = Rotation.LEFT;
    Tetromino z = new Tetromino(type, pos, r);

    assertEquals(type, z.type());
    assertEquals(pos, z.position());
    assertEquals(r, z.rotation());
  }

  /** Checks that the record constructor enforces non-null values for type, position and rotation.*/
  @Test
  void construction_rejectsNulls() {
    // don't know if Tetromino record enforces nulls.
    assertThrows(NullPointerException.class, () -> new Tetromino(null, new Cell(0,0), Rotation.UP));
    assertThrows(NullPointerException.class, () -> new Tetromino(TetrominoType.I, null, Rotation.UP));
    assertThrows(NullPointerException.class, () -> new Tetromino(TetrominoType.I, new Cell(0,0), null));
  }
