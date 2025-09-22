package com.example.tetris.services;

import com.example.tetris.helpers.EventSfx;
import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX based implementation of {@link AudioService}.
 * Goals:
 * - Loop background music via {@link MediaPlayer}.
 * - Preload short SFX as {@link AudioClip} for lower latency.
 * - Marshal all media calls to the JavaFX Application Thread
 * - Be idempotent and non-throwing. Should log and continue on failures.
 */
public final class JavaFxAudioService implements AudioService {

  private static final Logger LOG = Logger.getLogger(JavaFxAudioService.class.getName());

  // Resource paths under src/main/resources/audio/
  private static final String MUSIC_PATH = "audio/background.mp3";
  private static final Map<EventSfx, String> SFX_PATHS = Map.of(
      EventSfx.MOVE_TURN,     "audio/move-turn.wav",
      EventSfx.ERASE_LINE,    "audio/erase-line.wav",
      EventSfx.LEVEL_UP,      "audio/level-up.wav",
      EventSfx.GAME_FINISHED, "audio/game-finish.wav" // ← ensure filename matches your asset
  );

  // Rate limit for move/turn click spam. Adjust value for sound frequency.
  private static final Duration MOVE_TURN_MIN_INTERVAL = Duration.ofMillis(60);

  // Runtime flags
  private volatile boolean musicEnabled = true;
  private volatile boolean soundEnabled = true;

  // Music player kept for instant resume
  private MediaPlayer musicPlayer;

  // Preloaded SFX clips
  private final EnumMap<EventSfx, AudioClip> clips = new EnumMap<>(EventSfx.class);

  // Last allowed time for MOVE_TURN SFX to run
  private Instant lastMoveTurn = Instant.EPOCH;

  // Print each warning only once
  private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

  public JavaFxAudioService() {
    // Preload audio clips
    runOnFx(() -> {
      for (EventSfx esfx : EventSfx.values()) {
        String path = SFX_PATHS.get(esfx);
        if (path == null) continue;
        AudioClip clip = loadClip(path);
        if (clip != null) clips.put(esfx, clip);
      }
    });
  }

  // ---------------- AudioService ----------------

  /** Start or resume the background music if enabled
   * Creates the player on first use and loops infinitely   *
   */
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
          musicPlayer = new MediaPlayer(new Media(url.toExternalForm()));
          musicPlayer.setCycleCount(MediaPlayer.INDEFINITE); // loop forever
        }
        if (musicPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
          musicPlayer.play();
        }
      } catch (Throwable t) {
        LOG.log(Level.WARNING, "startMusic failed", t);
      }
    });
  }

  /**
   * Stops the background music if a player exists.
   * Keeps the player instance so restart is instant.
   */
  @Override
  public void stopMusic() {
    runOnFx(() -> {
      try {
        if (musicPlayer != null) {
          musicPlayer.stop();
          // Keep player instance for instant resume; real cleanup in dispose()
        }
      } catch (Throwable t) {
        LOG.log(Level.FINE, "stopMusic failed", t);
      }
    });
  }

  /**
   * Switch the music enabled flag and start/stop accordingly.
   * Safe to call repeatedly as start/stop is idempotent.
   */
  @Override
  public void setMusicEnabled(boolean enabled) {
    this.musicEnabled = enabled;
    if (enabled) startMusic();
    else stopMusic();
  }

  /**
   * Switch the sound effects enabled flag.
   * When disabled {@link #play(EventSfx)} becomes a no-op
   */
  @Override
  public void setSoundEnabled(boolean enabled) {
    this.soundEnabled = enabled;
  }

  /**
   * Play a sound effect for the given event.
   * MOVE_TURN is rate limited to avoid audio sounding terrible when piece is moved quickly.
   */
  @Override
  public void play(EventSfx event) {
    Objects.requireNonNull(event, "event");
    runOnFx(() -> {
      if (!soundEnabled) return;

      // Rate-limit for MOVE_TURN
      if (event == EventSfx.MOVE_TURN) {
        Instant now = Instant.now();
        if (Duration.between(lastMoveTurn, now).compareTo(MOVE_TURN_MIN_INTERVAL) < 0) {
          return;
        }
        lastMoveTurn = now;
      }

      AudioClip clip = clips.get(event);
      if (clip == null) {
        String path = SFX_PATHS.get(event);
        warnOnce("SFX asset not loaded for " + event + (path != null ? " (" + path + ")" : ""));
        return;
      }

      try {
        clip.play(); // default volume set on creation
      } catch (Throwable t) {
        LOG.log(Level.FINE, "play(" + event + ") failed", t);
      }
    });
  }

  /**
   * @return true if the music player exists and is currently playing.
   */
  @Override
  public boolean isMusicPlaying() {
    MediaPlayer mp = musicPlayer;
    return mp != null && mp.getStatus() == MediaPlayer.Status.PLAYING;
  }

  /**
   * Release all audio resources.
   * Stops and shuts down the music player and clears preloaded audio clips.
   */
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

  // ---------------- Helpers ----------------

  /** Run task on the JavaFx application thread. */
  private static void runOnFx(Runnable r) {
    if (Platform.isFxApplicationThread()) r.run();
    else Platform.runLater(r);
  }

  /** Load an {@link AudioClip} from classpath; return null if missing/unable to load. */
  private static AudioClip loadClip(String path) {
    try {
      URL url = resource(path);
      if (url == null) {
        warnOnce("Missing SFX asset: " + path);
        return null;
      }
      AudioClip ac = new AudioClip(url.toExternalForm());
      ac.setVolume(0.9); // default SFX volume, change if needed
      return ac;
    } catch (Throwable t) {
      LOG.log(Level.WARNING, "Failed to load clip " + path, t);
      return null;
    }
  }

  /**
   * Resolve resource in a way that works with modules & classpath.
   * Tries: current class loader, system class loader, then resource with leading "/".
   */
  private static URL resource(String path) {
    Class<?> anchor = JavaFxAudioService.class;

    URL url = anchor.getClassLoader().getResource(path);
    if (url == null) url = ClassLoader.getSystemResource(path);
    if (url == null && !path.startsWith("/")) url = anchor.getResource("/" + path);

    return url;
  }

  /** Log a warning once per unique message. */
  private static void warnOnce(String msg) {
    if (WARNED.add(msg)) {
      LOG.log(Level.WARNING, msg);
    }
  }
}
