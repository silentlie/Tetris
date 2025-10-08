package com.example.tetris.core.state;

/**
 * High-level input intents for controlling gameplay.
 *
 * <p>These values represent abstract player actions (rotate, move, drop) rather than
 * device-specific key codes. Map your UI input events to {@code GameIntent} first, then let the
 * game logic interpret them.</p>
 *
 * <p><b>Triggering model</b> (typical use in Tetris engines):</p>
 * <ul>
 *   <li>{@link #ROTATE_CW} / {@link #ROTATE_CCW}, {@link #MOVE_LEFT}, {@link #MOVE_RIGHT},
 *       and {@link #HARD_DROP} are usually handled as <i>discrete</i> (edge-triggered)
 *       actions—processed once per press.</li>
 *   <li>{@link #SOFT_DROP} is typically handled as a <i>held</i> (level-triggered)
 *       action—active while the input is down; it increases gravity while held.</li>
 * </ul>
 *
 * <p>Example mapping:</p>
 *
 * <p>This enum is stable and thread-safe; intent dispatching is up to the caller.</p>
 *
 * @since 1.0
 */
public enum GameIntent {

  /**
   * Rotate the active piece 90° counter-clockwise (left spin).
   */
  ROTATE_CCW,

  /**
   * Rotate the active piece 90° clockwise (right spin).
   */
  ROTATE_CW,

  /**
   * Shift the active piece one column to the left.
   */
  MOVE_LEFT,

  /**
   * Shift the active piece one column to the right (typically discrete with DAS/ARR handled
   * elsewhere).
   */
  MOVE_RIGHT,

  /**
   * Increase gravity while held, causing the active piece to fall faster; does not instantly lock
   * or snap to the floor.
   */
  SOFT_DROP,

  /**
   * Instantly drop the active piece to the lowest valid position and lock it.
   */
  HARD_DROP,

  /**
   * Active piece fall faster as level increase.
   */
  GRAVITY_DROP,
}
