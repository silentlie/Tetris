package com.example.tetris.helpers;

/**
 * Gameplay events that trigger the sound effects.
 *
 */
public enum EventSfx {
  // game events that map to the sfx assets in /audio
  /** Tetromino moved or rotated */
  MOVE_TURN,

  /** One or more lines are cleared */
  ERASE_LINE,

  /** Player level increased */
  LEVEL_UP,

  /** Game over */
  GAME_FINISHED
}
