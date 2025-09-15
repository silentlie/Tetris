package com.example.tetris.models;

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
  // Defaults
  public static final int DEFAULT_WIDTH = 10;
  public static final int DEFAULT_HEIGHT = 20;
  public static final int DEFAULT_LEVEL = 1;
  public static final boolean DEFAULT_MUSIC = true;
  public static final boolean DEFAULT_SOUND = true;
  public static final boolean DEFAULT_EXTEND_MODE = false;
  public static final PlayerType DEFAULT_PLAYER1 = PlayerType.HUMAN;
  public static final PlayerType DEFAULT_PLAYER2 = PlayerType.HUMAN;

  // Enum with labels (for UI)
  public enum PlayerType {
    HUMAN("Human"), AI("AI"), EXTERNAL("External");
    private final String label;
    PlayerType(String label) { this.label = label; }
    public String label() { return label; }
    @Override public String toString() { return label; }
  }

  // Canonical constructor for validation
  public Configuration {
    if (width  < 1) width  = DEFAULT_WIDTH;
    if (height < 1) height = DEFAULT_HEIGHT;
    if (level  < 1) level  = DEFAULT_LEVEL;

    if (player1 == null) player1 = DEFAULT_PLAYER1;
    if (player2 == null) player2 = DEFAULT_PLAYER2;

  }

  // default config
  public Configuration() {
    this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_LEVEL,
        DEFAULT_MUSIC, DEFAULT_SOUND, DEFAULT_EXTEND_MODE,
        DEFAULT_PLAYER1, DEFAULT_PLAYER2);
  }
}
