package com.example.tetris.services;


/**
 * Facade for all game audio.
 */

public interface AudioService {


  // Starts background music when enabled.
  void startMusic();

  // Stops background music if it's playing.
  void stopMusic();

  // Enables or disables background music at runtime.
  void setMusicEnabled(boolean enabled);

  // Enables or disables sound effects globally
  void setSoundEnabled(boolean enabled);

  // Plays a short sound effect for the given event ie moves and turns of the shapes.
  void play(EventSfx event);

  // Indicator to see if music is currently playing.
  boolean isMusicPlaying();

  // Releases any underlying audio resources. Used for app shutdown.
  default void dispose() {

  }
}