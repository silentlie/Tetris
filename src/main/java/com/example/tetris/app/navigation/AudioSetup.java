package com.example.tetris.helpers;

import com.example.tetris.services.AudioService;
import com.example.tetris.services.ConfigurationService;
import javafx.stage.Stage;

/** Centralizes audio startup/lifecycle wiring so Main stays lean. */
public final class AudioSetup {
  private AudioSetup() {}

  /** Apply persisted config, start music, and wire stage lifecycle handlers. */
  public static void init(Stage primaryStage,
      ConfigurationService configurationService,
      AudioService audio) {
    // Apply persisted configuration (cached, non-null per contract)
    try {
      var cfg = configurationService.getCache();
      audio.setMusicEnabled(cfg.music());
      audio.setSoundEnabled(cfg.sound());
    } catch (Exception e) {
      System.err.println("Configuration read failed: " + e.getMessage());
    }

    // Start background music if enabled
    audio.startMusic();

    // Pause/resume on minimize/restore
    primaryStage.iconifiedProperty().addListener((obs, wasMin, isMin) -> {
      if (isMin) {
        audio.stopMusic();
      } else if (!audio.isMusicPlaying()) {
        audio.startMusic();
      }
    });

//    // Pause music when window loses focus and resume on refocus
//    primaryStage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
//      if (!isFocused) {
//        audio.stopMusic();
//      } else if (!audio.isMusicPlaying()) {
//        audio.startMusic();
//      }
//    });

    // Dispose on window close
    primaryStage.setOnCloseRequest(evt -> {
      try {
        audio.dispose();
      } catch (Exception ignored) {}
    });
  }

  /** Ensure audio is cleaned up on app stop() as an extra safety net. */
  public static void shutdown(AudioService audio) {
    if (audio == null) return;
    try {
      audio.dispose();
    } catch (Exception ignored) {}
  }
}
