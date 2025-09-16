package com.example.tetris.services;

import com.example.tetris.services.EventSfx;

/**
 * Facade for all game audio. Implementation must be non-throwing and ide impotent
 * Threading: methods are expected to be called on the Javafx application thread
 */

public interface AudioService {

  // Start background music if enabled; no-op if already playing or disabled
  void startMusic();

  // Stop background music; safe to call when already stopped.
  void stopMusic();

  // Enable or disable background music at runtime. If enabled, music should begin playing.
  void setMusicEnabled(boolean enabled);

  //Enable or disable sound effects globally. When disabled, play is a no op
  void setSoundEnabled(boolean enabled);

  // Play a short sound effect for the given event; must be non-blocking and low-latency.
  void play(EventSfx event);

  //Best effort indicator for ui/status
  boolean isMusicPlaying();

  // Release any underlying audio resources called on app shutdown.
  default void dispose() {
    // optional for implementation
  }
}