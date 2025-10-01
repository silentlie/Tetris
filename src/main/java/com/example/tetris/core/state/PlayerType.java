package com.example.tetris.core.state;

/**
 * Player categories available in the game.
 *
 * <p>Each constant has a human-readable {@code label} exposed via {@link #label()} and
 * {@link #toString()}, making it convenient to show directly in UI menus, configuration dialogs,
 * logs, or tooltips.</p>
 *
 * <p>This enum is stable and can be safely serialized (e.g. in configuration files).</p>
 */
public enum PlayerType {

  /**
   * A player controlled by a human user through the UI.
   */
  HUMAN("Human"),

  /**
   * A built-in computer-controlled player (Ai) that makes automated moves.
   */
  AI("Ai"),

  /**
   * A player controlled by an external process or integration (for example, a network client, bot
   * framework, or IPC channel).
   */
  EXTERNAL("External");

  private final String label;

  /**
   * Constructs a player type with the given display label.
   *
   * @param label human-readable text representing this type (never {@code null})
   */
  PlayerType(String label) {
    this.label = label;
  }

  /**
   * Returns the human-readable label for this type.
   *
   * @return display label (never {@code null})
   */
  public String label() {
    return label;
  }

  /**
   * Returns the human-readable label. Equivalent to {@link #label()}.
   *
   * @return display label (never {@code null})
   */
  @Override
  public String toString() {
    return label;
  }
}
