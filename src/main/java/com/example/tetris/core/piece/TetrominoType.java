package com.example.tetris.core.piece;

import java.util.concurrent.ThreadLocalRandom;
import javafx.scene.paint.Color;

/**
 * Represents the seven standard tetromino types used in Tetris: {@code I}, {@code O}, {@code T},
 * {@code S}, {@code Z}, {@code J}, and {@code L}.
 *
 * <p>Each tetromino type is associated with a distinct {@link Color}
 * that is typically used for rendering the tetromino on the board.</p>
 *
 * <p>Utility methods are provided for retrieving the color and selecting
 * a random tetromino type.</p>
 */
public enum TetrominoType {

  /**
   * Straight line tetromino ("I" shape).
   */
  I(Color.CYAN),

  /**
   * Square tetromino ("O" shape).
   */
  O(Color.YELLOW),

  /**
   * T-shaped tetromino ("T" shape).
   */
  T(Color.MAGENTA),

  /**
   * Skew tetromino leaning right ("S" shape).
   */
  S(Color.GREEN),

  /**
   * Skew tetromino leaning left ("Z" shape).
   */
  Z(Color.RED),

  /**
   * Left-facing L tetromino ("J" shape).
   */
  J(Color.BLUE),

  /**
   * Right-facing L tetromino ("L" shape).
   */
  L(Color.ORANGE);

  private final Color color;

  TetrominoType(Color color) {
    this.color = color;
  }

  /**
   * Selects and returns a random {@code TetrominoType}.
   *
   * <p>This uses {@link ThreadLocalRandom} to ensure thread-safe randomness.</p>
   *
   * <p>Example usage:</p>
   * <pre>
   *   TetrominoType random = TetrominoType.randomType();
   *   System.out.println(random); // e.g. "T" or "Z"
   * </pre>
   *
   * @return a randomly chosen {@code TetrominoType}, never {@code null}
   */
  public static TetrominoType randomType() {
    TetrominoType[] values = TetrominoType.values();
    return values[ThreadLocalRandom.current().nextInt(values.length)];
  }

  /**
   * Returns the display {@link Color} associated with this tetromino type.
   *
   * <p>This color is typically used for rendering the tetromino on the board.</p>
   *
   * @return the color assigned to this tetromino type, never {@code null}
   */
  public Color getColor() {
    return color;
  }
}
