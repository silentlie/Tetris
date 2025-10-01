package com.example.tetris.core.config;

import com.example.tetris.core.state.PlayerType;
import java.util.Objects;

/**
 * Immutable game configuration.
 *
 * <p>This {@code record} captures board geometry, starting level, audio flags,
 * optional extended mode, and player types for two participants. Instances are validated on
 * construction; out-of-range values trigger an {@link IllegalArgumentException}.</p>
 *
 * <p><strong>Invariants & ranges:</strong>
 * <ul>
 *   <li>{@code width} in [{@link #MIN_WIDTH}, {@link #MAX_WIDTH}]</li>
 *   <li>{@code height} in [{@link #MIN_HEIGHT}, {@link #MAX_HEIGHT}]</li>
 *   <li>{@code level} in [{@link #MIN_LEVEL}, {@link #MAX_LEVEL}]</li>
 *   <li>{@code player1} and {@code player2} are non-null</li>
 * </ul>
 *
 * <p><strong>Defaults:</strong> See {@code DEFAULT_*} constants. The no-arg constructor
 * builds a fully validated configuration using those defaults.</p>
 *
 * @param width      board width in cells (see {@link #MIN_WIDTH}/{@link #MAX_WIDTH})
 * @param height     board height in cells (see {@link #MIN_HEIGHT}/{@link #MAX_HEIGHT})
 * @param level      starting difficulty level (see {@link #MIN_LEVEL}/{@link #MAX_LEVEL})
 * @param music      background music enabled
 * @param sound      sound effects enabled
 * @param extendMode extended gameplay rule/board variant enabled
 * @param player1    primary player type (non-null)
 * @param player2    secondary player type (non-null)
 */
public record Configuration(
    int width,
    int height,
    int level,
    boolean music,
    boolean sound,
    boolean extendMode,
    PlayerType player1,
    PlayerType player2
) {
  public static final int NEXT_QUEUE_SIZE = 3;

  /**
   * Default board width (cells).
   */
  public static final int DEFAULT_WIDTH = 10;
  /**
   * Default board height (cells).
   */
  public static final int DEFAULT_HEIGHT = 20;
  /**
   * Default starting level.
   */
  public static final int DEFAULT_LEVEL = 1;
  /**
   * Default: background music enabled.
   */
  public static final boolean DEFAULT_MUSIC = true;
  /**
   * Default: sound effects enabled.
   */
  public static final boolean DEFAULT_SOUND = true;
  /**
   * Default: extended mode disabled.
   */
  public static final boolean DEFAULT_EXTEND_MODE = false;
  /**
   * Default player 1 type.
   */
  public static final PlayerType DEFAULT_PLAYER1 = PlayerType.HUMAN;
  /**
   * Default player 2 type.
   */
  public static final PlayerType DEFAULT_PLAYER2 = PlayerType.HUMAN;

  /**
   * Minimum and maximum allowed board width (cells).
   */
  public static final int MIN_WIDTH = 5;
  public static final int MAX_WIDTH = 15;
  /**
   * Minimum and maximum allowed board height (cells).
   */
  public static final int MIN_HEIGHT = 15;
  public static final int MAX_HEIGHT = 30;
  /**
   * Minimum and maximum allowed starting level.
   */
  public static final int MIN_LEVEL = 1;
  public static final int MAX_LEVEL = 10;

  /**
   * Canonical constructor with validation enforcing documented invariants.
   *
   * @throws IllegalArgumentException if {@code width}, {@code height}, or {@code level} is outside
   *                                  the allowed range
   * @throws NullPointerException     if {@code player1} or {@code player2} is {@code null}
   */
  public Configuration {
    if (width < MIN_WIDTH || width > MAX_WIDTH) {
      throw new IllegalArgumentException(
          "width out of range: " + width + " (allowed " + MIN_WIDTH + ".." + MAX_WIDTH + ")");
    }
    if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
      throw new IllegalArgumentException(
          "height out of range: " + height + " (allowed " + MIN_HEIGHT + ".." + MAX_HEIGHT + ")");
    }
    if (level < MIN_LEVEL || level > MAX_LEVEL) {
      throw new IllegalArgumentException(
          "level out of range: " + level + " (allowed " + MIN_LEVEL + ".." + MAX_LEVEL + ")");
    }
    Objects.requireNonNull(player1, "player1");
    Objects.requireNonNull(player2, "player2");
  }

  /**
   * Convenience constructor that builds a configuration using the {@code DEFAULT_*} constants.
   * Values are validated by the canonical constructor.
   */
  public Configuration() {
    this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_LEVEL, DEFAULT_MUSIC, DEFAULT_SOUND,
        DEFAULT_EXTEND_MODE, DEFAULT_PLAYER1, DEFAULT_PLAYER2);
  }
}
