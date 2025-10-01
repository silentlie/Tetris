package com.example.tetris.services;

import com.example.tetris.helpers.EventSfx;

/**
 * Small facade for game audio functions.
 *
 * Controllers talk to this interface instead of JavaFx media classes directly.
 */

public interface AudioService {

  // Starts background music. Idempotent so it won't restart if already playing.
  void startMusic();

  // Stop background music
  void stopMusic();

  // Enable/disable background music at runtime.
  void setMusicEnabled(boolean enabled);

  // Enable/disable sound effects at runtime.
  void setSoundEnabled(boolean enabled);

  // Play a sound effect for the given event ie shape moves etc
  void play(EventSfx event);

  // Check if music is currently playing.
  boolean isMusicPlaying();

  // Release all audio resources for app shutdown.
  void dispose();

}
