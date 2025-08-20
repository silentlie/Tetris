package com.example.tetris.models;

import java.util.concurrent.ThreadLocalRandom;
import javafx.scene.paint.Color;

/**
 * Represents the seven standard tetromino types used in Tetris: I, O, T, S, Z, J, and L.
 *
 * <p>Each type is associated with a distinct {@link Color} that is
 * typically used for rendering the tetromino on the board.</p>
 */
public enum TetrominoType {
  I(Color.CYAN),
  O(Color.YELLOW),
  T(Color.MAGENTA),
  S(Color.GREEN),
  Z(Color.RED),
  J(Color.BLUE),
  L(Color.ORANGE);

  private final Color color;

  TetrominoType(Color color) {
    this.color = color;
  }

  public Color getColor() {
    return color;
  }

  /**
   * Selects and returns a random tetromino type.
   *
   * <p>This uses {@link ThreadLocalRandom} to ensure thread-safe randomness.</p>
   *
   * @return a randomly chosen tetromino type
   */
  public static TetrominoType randomType() {
    TetrominoType[] values = TetrominoType.values();
    return values[ThreadLocalRandom.current().nextInt(values.length)];
  }
}