package com.example.tetris.core.state;

/**
 * Enumerates the available player types in the game.
 *
 * <p>Each constant carries a human-readable {@code label} that can be used directly in UI
 * components (menus, tooltips, logs) via {@link #label()} or {@link #toString()}.</p>
 */
public enum PlayerType {

  /**
   * A player controlled by a human user via the UI.
   */
  HUMAN("Human"),

  /**
   * A built-in computer player (AI) that makes automated moves.
   */
  AI("AI"),

  /**
   * A player controlled by an external process or integration (e.g., network client, bot framework,
   * or IPC).
   */
  EXTERNAL("External");

  private final String label;

  /**
   * Constructs an enum constant with a display label.
   *
   * @param label human-readable text for this player type
   */
  PlayerType(String label) {
    this.label = label;
  }

  /**
   * Returns the human-readable label for this player type.
   *
   * @return display label (never {@code null})
   */
  public String label() {
    return label;
  }

  /**
   * Returns the human-readable label. Equivalent to {@link #label()}.
   *
   * @return display label
   */
  @Override
  public String toString() {
    return label;
  }
}
