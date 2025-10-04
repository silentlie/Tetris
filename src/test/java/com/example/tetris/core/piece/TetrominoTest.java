package com.example.tetris.core.piece;


import static org.junit.jupiter.api.Assertions.*;

import com.example.tetris.core.board.Cell;
import com.example.tetris.core.piece.Rotation;
import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.piece.TetrominoType;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit testing for the {@link Tetromino} record.
 * Includes: movement, rotation, absolute shape checks, factory invariants,
 * construction invariants including nulls, and immutability.
 */

class TetrominoTest {

  private Cell spawn;

  /** Use a fixed spawn point for deterministic factory tests*/
  @BeforeEach
  void setUp() {
    spawn = TestHelpers.spawn();
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
   * Factory should return a non-null with a valid type, rotation should equal UP and position
   * should equal spawn.
   */
  @Test
  void randomTetromino() {
    Tetromino t = Tetromino.randomTetromino(spawn);
    assertNotNull(t, "randomTetromino should not return null");
    assertNotNull(t.type(), "type should not be null");
    assertEquals(Rotation.UP, t.rotation(), "random tetromino must start UP");
    assertEquals(spawn, t.position(), "random tetromino must use spawn position");
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

  /** Factory must also reject null values for spawn position. */
  @Test
  void randomTetromino_rejectsNullSpawn() {
    assertThrows(NullPointerException.class, () -> Tetromino.randomTetromino(null));
  }

}