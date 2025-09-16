package com.example.tetris.services;

import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFx based implementation of AudioService.
 * - Music is looped via MediaPlayer (MP3)
 * - SFX: preloaded AudioClip (WAV)
 *
 */

public final class JavaFxAudioService implements AudioService {

  private static final Logger LOG = Logger.getLogger(JavaFxAudioService.class.getName());

  // Resource paths
  private static final String MUSIC_PATH = "audio/background.mp3";
  private static final Map<EventSfx, String> SFX_PATHS = Map.of(
      EventSfx.MOVE_TURN, "audio/move-turn.wav",
      EventSfx.ERASE_LINE, "audio/erase-line.wav",
      EventSfx.LEVEL_UP,  "audio/level-up.wav",
      EventSfx.GAME_FINISHED, "audio/game-finish.wav"
  );

  // Rate-limit for MOVE_TURN spam (can be tweaked to sound better)
  private static final Duration MOVE_TURN_MIN_INTERVAL = Duration.ofMillis(60); // modify for sound frequency.

  private volatile boolean musicEnabled = true;
  private volatile boolean soundEnabled = true;

  private MediaPlayer musicPlayer;
  private final EnumMap<EventSfx, AudioClip> clips = new EnumMap<>(EventSfx.class);
  private Instant lastMoveTurn = Instant.EPOCH;

  private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

  public JavaFxAudioService() {
    // Preload SFX on FX thread
    runOnFx(() -> {
      for (var e: EventSfx.values()) {
        var path = SFX_PATHS.get(e);
        var clip = loadClip(path);
        if (clip != null) clips.put(e, clip);
      }
    });
  }
  // ------------- AudioService ----------------

  @Override
  public void startMusic() {
    runOnFx(() -> {
      if (!musicEnabled) return;
      try {
        if (musicPlayer == null) {
          URL url = resource(MUSIC_PATH);
          if (url == null) {
            warnOnce("Missing music asset: " + MUSIC_PATH);
            return;
          }
          musicPlayer = new MediaPlayer((new Media(url.toExternalForm())));
          musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        }
        var status = musicPlayer.getStatus();
        if (status != MediaPlayer.Status.PLAYING) {
          musicPlayer.play();
        }
      } catch (Throwable t) {
        LOG.log(Level.WARNING, "startMusic failed", t);
      }
    });
  }

  @Override
  public void stopMusic() {
    runOnFx(() -> {
      try {
        if (musicPlayer != null) {
          musicPlayer.stop();
          // keep the player so startMusic() is instant. Dispose is only on dispose()
        }
      } catch (Throwable t) {
        LOG.log(Level.FINE, "stopMusic failed", t);
      }
    });
  }

  @Override
  public void setMusicEnabled(boolean enabled) {
    this.musicEnabled = enabled;
    if (enabled) startMusic();
    else stopMusic();
  }

  @Override
  public void setSoundEnabled(boolean enabled) {
    this.soundEnabled = enabled;
  }

  @Override
  public void play(EventSfx event) {
    Objects.requireNonNull(event, "event");
    runOnFx(() -> {
      if (!soundEnabled) return;
      if (event == EventSfx.MOVE_TURN) {
        var now = Instant.now();
        if (Duration.between(lastMoveTurn, now).compareTo(MOVE_TURN_MIN_INTERVAL) < 0) return;
        lastMoveTurn = now;
      }

      var clip = clips.get(event);
      if (clip == null) {
        warnOnce("SFX asset not loaded for " + event + " (" + SFX_PATHS.get(event) + ")");
        return;
      }
      try {
        clip.play();
      } catch (Throwable t) {
        LOG.log(Level.FINE, "play(" + event + ") failed", t);
      }
    });
  }

  @Override
  public boolean isMusicPlaying() {
    var p = musicPlayer;
    return p != null && p.getStatus() == MediaPlayer.Status.PLAYING;
  }

  @Override
  public void dispose() {
    runOnFx(() -> {
      try {
        if (musicPlayer != null) {
          musicPlayer.stop();
          musicPlayer.dispose();
          musicPlayer = null;
        }
        clips.clear();
      } catch (Throwable t) {
        LOG.log(Level.FINE, "dispose failed", t);
      }
    });
  }

  // -------------- helpers ------------------

  private static void runOnFx(Runnable r) {
    if (Platform.isFxApplicationThread()) r.run();
    else Platform.runLater(r);
  }

  private static AudioClip loadClip(String path) {
    try {
      URL url = resource(path);
      if (url == null) {
        warnOnce("Missing SFX asset: " + path);
        return null;
      }
      AudioClip ac = new AudioClip(url.toExternalForm());
      ac.setVolume(0.9);
      return ac;
    } catch (Throwable t) {
      LOG.log(Level.WARNING, "Failed to load clip " + path, t);
      return null;
    }
  }

//  private static URL resource(String path) {
//    return Thread.currentThread().getContextClassLoader().getResource(path);
//  }

  private static URL resource(String path) {
    Class<?> anchor = JavaFxAudioService.class;

    URL url = anchor.getClassLoader().getResource(path);   // module-aware
    if (url == null) url = ClassLoader.getSystemResource(path);
    if (url == null && !path.startsWith("/")) url = anchor.getResource("/" + path);

    return url;
  }


  private static void warnOnce(String msg) {
    if (WARNED.add(msg)) {
      LOG.log(Level.WARNING, msg);
    }
  }


}
