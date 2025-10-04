package com.example.tetris.core.piece;

import static org.junit.jupiter.api.Assertions.*;
import com.example.tetris.core.board.Cell;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper functions to be called in {@link TetrominoTest} for ease of use and readability.
 * Helps keep consistency for the tests.
 */

final class TestHelpers {

  /** @return a fixed spawn cell. */
  static Cell spawn() {
    return new Cell(5, 0);
  }

  /** Factory to create a tetromino.*/
  static Tetromino of(TetrominoType t, int x, int y, Rotation r) {
    return new Tetromino(t, new Cell(x, y), r);
  }

  /** Converts a list into a set.*/
  static Set<Cell> asSet(List<Cell> cells) {
    return new HashSet<>(cells);
  }

  /** Compares an expected set of cells vs the actual list returned by the code under test.
   * Does this after converting the list to a set.
   */
  static void assertCellSetEquals(Set<Cell> expected, List<Cell> actual) {
    assertEquals(expected, asSet(actual));
  }

  static void assertMovedTo(Tetromino original, Tetromino moved, Cell expectedPos) {
    assertNotSame(original, moved, "must return a new instance");
    assertEquals(expectedPos, moved.position(), "pivot should update correctly");
    assertEquals(original.type(), moved.type(), "type must be preserved");
    assertEquals(original.rotation(), moved.rotation(), "rotation must be preserved");
  }

  static void assertRotatedTo(Tetromino original, Tetromino rotated, Rotation expectedRotation) {
    assertNotSame(original, rotated, "rotateXxx() must return a new instance");
    assertEquals(expectedRotation, rotated.rotation(), "rotation must be updated correctly");
    assertEquals(original.type(), rotated.type(), "type must be preserved");
    assertEquals(original.position(), rotated.position(), "position must be preserved");
  }
}
