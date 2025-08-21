package com.example.tetris.helpers;

/**
 * Callbacks that drive the Tetris game loop.
 *
 * <p>Implemented by scenes/controllers (e.g. {@code Play}) so a generic {@link GameTimer}
 * can orchestrate gravity, locking, rendering, and lifecycle events without subclassing.</p>
 *
 * <h2>Execution model</h2>
 * <ul>
 *   <li>Methods are invoked by {@link GameTimer} on the game loop thread.</li>
 *   <li>Implementations should be fast and non-blocking.</li>
 * </ul>
 */
public interface GameLoopCallbacks {

  /**
   * Attempts to advance the active piece by one row (soft drop).
   *
   * <p>This method should not lock the piece; it only moves it if the space below is free. Scoring
   * for
   * soft-drop steps (if any) is handled by the implementation.
   *
   * @return {@code true} if the piece moved down one row; {@code false} if movement was blocked.
   */
  boolean softDrop();

  /**
   * Locks the current piece into the well, clears any completed lines, updates score/state, spawns
   * the next piece, and performs lock-out detection.
   *
   * @return {@code true} if the game has ended as a result of locking (e.g., lock out);
   *     {@code false} otherwise.
   */
  boolean lock();

  /**
   * Invoked exactly once when the game ends (e.g., lock out). Implementations should perform
   * teardown, persist scores, and transition UI state. This method must be idempotent.
   */
  void endGame();

  /**
   * Renders the current state.
   *
   * <p>Must be fast and side effect free with respect to game logic. Callers may choose to gate
   * invocations based on {@link #markDirty()} to avoid redundant work.
   */
  void render();

  /**
   * Marks the view as needing a redraw on the next frame.
   *
   * <p>Implementations typically set an internal dirty flag read by the render scheduler. This
   * method
   * should be safe to call frequently.
   */
  void markDirty();
}
