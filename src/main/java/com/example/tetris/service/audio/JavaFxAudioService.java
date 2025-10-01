package com.example.tetris.service.audio;

import java.net.URL;
import java.util.EnumMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * JavaFX-based {@link AudioService} implementation.
 *
 * <p>Uses {@link MediaPlayer} for looping background music (BGM) and {@link AudioClip} for
 * low-latency sound effects (SFX). All JavaFX media operations that require the FX thread are
 * marshalled via {@link Platform#runLater(Runnable)} as needed.</p>
 *
 * <h2>Threading</h2>
 * <ul>
 *   <li>BGM operations (load/play/stop/dispose) are performed on the JavaFX application thread.
 *   </li>
 *   <li>SFX {@link AudioClip} playback is non-blocking and may occur off the FX thread.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Constructor loads BGM and SFX resources on the FX thread.</li>
 *   <li>{@link #setBgm(boolean)} plays/stops looping BGM; {@link #setSound(boolean)} toggles SFX
 *       and stops any currently playing clips when disabling.</li>
 *   <li>{@link #play(SoundCue)} is a no-op when SFX are disabled or the clip is missing.</li>
 *   <li>{@link #close()} stops/disposes BGM and clears clip references; it is idempotent.</li>
 * </ol>
 *
 * <p>Failures to load individual assets are logged and do not prevent the service from
 * functioning for other assets.</p>
 *
 * @since 1.0
 */
public final class JavaFxAudioService implements AudioService {

  private static final Logger LOG = Logger.getLogger(JavaFxAudioService.class.getName());

  /**
   * Classpath-relative path to the background-music file.
   */
  private static final String BGM_PATH = "background.mp3";

  /**
   * Preloaded SFX clips keyed by semantic cue.
   */
  private final EnumMap<SoundCue, AudioClip> clips = new EnumMap<>(SoundCue.class);

  /**
   * Whether SFX playback is enabled. BGM enablement is controlled by the player state.
   */
  private volatile boolean soundEnabled = true;

  /**
   * Looping media player for background music. Created on FX thread.
   */
  private MediaPlayer bgm;

  /**
   * Constructs the service and loads audio resources.
   *
   * <p>Resource loading is scheduled onto the FX thread. Missing or malformed resources are
   * logged at {@link Level#WARNING} (SFX) or {@link Level#SEVERE} (BGM) and skipped.</p>
   */
  public JavaFxAudioService() {
    runOnFx(() -> {
      loadBgm();
      loadClips();
    });
  }

  /**
   * Execute {@code r} immediately if on the FX thread; otherwise schedule via
   * {@link Platform#runLater(Runnable)}.
   */
  private static void runOnFx(Runnable r) {
    if (Platform.isFxApplicationThread()) {
      r.run();
    } else {
      Platform.runLater(r);
    }
  }

  /**
   * Load all SFX {@link AudioClip}s declared by {@link SoundCue}.
   *
   * <p>Clips that fail to load are skipped; callers of {@link #play(SoundCue)} will see a
   * warning.</p>
   */
  private void loadClips() {
    for (SoundCue cue : SoundCue.values()) {
      try {
        URL url = JavaFxAudioService.class.getResource(cue.path);
        AudioClip clip = new AudioClip(
            Objects.requireNonNull(url, "Missing SFX: " + cue.path).toExternalForm());
        clips.put(cue, clip);
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Failed to load SFX: " + cue, e);
      }
    }
  }

  /**
   * Load and prepare the looping BGM {@link MediaPlayer}.
   *
   * <p>On success, the player is configured to loop indefinitely; playback only starts when
   * {@link #setBgm(boolean)} is called with {@code true}.</p>
   */
  private void loadBgm() {
    try {
      URL url = this.getClass().getResource(BGM_PATH);
      Media media = new Media(
          Objects.requireNonNull(url, "Missing BGM: " + BGM_PATH).toExternalForm());
      bgm = new MediaPlayer(media);
      bgm.setCycleCount(MediaPlayer.INDEFINITE);
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Failed to load BGM from " + BGM_PATH, e);
    }
  }

  /**
   * Enable or disable looping background music.
   *
   * <p>When enabled and the BGM resource loaded successfully, playback starts (or resumes) on
   * the FX thread. When disabled, playback stops promptly. If BGM failed to initialize, a severe
   * log entry is produced and the call is a no-op.</p>
   *
   * @param enabled {@code true} to play; {@code false} to stop
   */
  @Override
  public void setBgm(boolean enabled) {
    if (bgm == null) {
      LOG.severe("BGM not initialized");
      return;
    }
    runOnFx(() -> {
      if (enabled) {
        bgm.play();
      } else {
        bgm.stop();
      }
    });
  }

  /**
   * Enable or disable sound effects.
   *
   * <p>Disabling will immediately stop any currently playing clips. Enabling affects only
   * subsequent {@link #play(SoundCue)} calls.</p>
   *
   * @param enabled {@code true} to allow SFX playback; {@code false} to mute SFX
   */
  @Override
  public void setSound(boolean enabled) {
    soundEnabled = enabled;
    if (!enabled) {
      // SAFE TWEAK: iterate loaded clips directly.
      for (AudioClip clip : clips.values()) {
        clip.stop();
      }
    }
  }

  /**
   * Play the sound effect associated with the given {@link SoundCue}, if available and enabled.
   *
   * <p>Playback is asynchronous and returns immediately. If SFX are disabled or the clip could
   * not be loaded, this method is a no-op (with a warning for the latter).</p>
   *
   * @param event semantic identifier of the effect to play (must not be {@code null})
   *
   * @throws NullPointerException if {@code event} is {@code null}
   */
  @Override
  public void play(SoundCue event) {
    Objects.requireNonNull(event, "event");
    if (!soundEnabled) {
      return;
    }
    AudioClip clip = clips.get(event);
    if (clip != null) {
      clip.play();
    } else {
      LOG.warning("SFX not loaded for cue: " + event);
    }
  }

  /**
   * Stop and dispose of BGM and clear SFX clip references.
   *
   * <p>After this call returns, the instance should not be reused. The method is idempotent.</p>
   */
  @Override
  public void close() {
    runOnFx(() -> {
      if (bgm != null) {
        bgm.stop();
        bgm.dispose();
        bgm = null;
      }
      clips.clear();
    });
  }
}
