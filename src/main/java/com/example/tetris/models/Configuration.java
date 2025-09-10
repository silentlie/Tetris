package com.example.tetris.models;
/*
TASK
1. Create Configuration record
File: src/main/java/com/example/tetris/models/Configuration.java
Define a configuration record with fields: width, height, level, music, sound, extend mode, player 1, player 2
Add explicit canonical constructor validating ranges (e.g., width/height ≥ 1, level ≥ 1)
Provide a no-argument constructor with default values:
width = 10
height = 20
level = 1
music = true
sound = true
extendMode = false
player 1 = HUMAN
player 2 = HUMAN
Define an enum PlayerType with values: HUMAN, AI, EXTERNAL
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

  // Default settings
  public static final int DEFAULT_WIDTH = 10;
  public static final int DEFAULT_HEIGHT = 20;
  public static final int DEFAULT_LEVEL = 1;
  public static final boolean DEFAULT_MUSIC = true;
  public static final boolean DEFAULT_SOUND = true;
  public static final boolean DEFAULT_EXTEND_MODE = false;
  public static final PlayerType DEFAULT_PLAYER1 = PlayerType.HUMAN;
  public static final PlayerType DEFAULT_PLAYER2 = PlayerType.HUMAN;

  // Canonical Constructor for Validation
  public Configuration{
    if (width < 1) {
      throw new IllegalArgumentException("Width must be greater than or equal to 1");
    }
    if (height < 1) {
      throw new IllegalArgumentException("Height must be greater than or equal to 1");
    }
    if (level < 1) {
      throw new IllegalArgumentException("Level must be greater than or equal to 1");
    }
    if (player1 == null || player2 == null) {
      throw new IllegalArgumentException("Player type must not be null");
    }
  }

  // No-Argument Constructor with Default Values
  public Configuration() {
    this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_LEVEL, DEFAULT_MUSIC, DEFAULT_SOUND, DEFAULT_EXTEND_MODE, DEFAULT_PLAYER1, DEFAULT_PLAYER2);
  }

  // ENUM
  public enum PlayerType {
    HUMAN, AI, EXTERNAL
  }

}






