package com.example.tetris.service.audio;

/**
 * Enumerates short sound effects triggered by Tetris gameplay events.
 *
 * <p>Each constant carries a {@link #path} which is the <em>relative filename</em> of the
 * corresponding audio asset (e.g., {@code "erase-line.wav"}). The concrete audio layer (e.g.,
 * {@code JavaFxAudioService}) is responsible for resolving this filename to a classpath resource,
 * applying volume/throttling, and playing it.</p>
 *
 * <h2>Resource layout</h2>
 * Place audio files under your resources folder and have the audio service prepend its configured
 * base directory. For example, with a base folder {@code "/com/example/tetris/audio/sfx/"} the enum
 * value {@link #ERASE_LINE} resolves to {@code "/com/example/tetris/audio/sfx/erase-line.wav"} on
 * the classpath.
 *
 * <h2>Typical use</h2>
 * <pre>{@code
 * // Inside your audio service (JavaFX example):
 * URL url = AudioService.class.getResource(BASE + event.path());
 * AudioClip clip = new AudioClip(Objects.requireNonNull(url).toExternalForm());
 * clip.play();
 *
 * // From game code:
 * audioService.play(SoundCue.ERASE_LINE);
 * }</pre>
 *
 * <p><strong>Note:</strong> This enum is intentionally UI/tech-agnostic. Keep file
 * resolution and playback inside your audio service so assets can be switched without touching
 * gameplay code.</p>
 *
 * @since 1.0
 */
public enum SoundCue {

  /**
   * Short tick/click for a piece movement or rotation.
   *
   * <p>File: {@code move-turn.wav}</p>
   */
  MOVE_TURN("move-turn.wav"),

  /**
   * Line-clear sound for one or more completed rows.
   *
   * <p>File: {@code erase-line.wav}</p>
   */
  ERASE_LINE("erase-line.wav"),

  /**
   * Stinger played when the player advances to a new level.
   *
   * <p>File: {@code level-up.wav}</p>
   */
  LEVEL_UP("level-up.wav"),

  /**
   * End-of-game cue when the board locks out.
   *
   * <p>File: {@code game-finish.wav}</p>
   */
  GAME_FINISH("game-finish.wav");

  /**
   * Relative filename of the audio asset (no leading slash). The audio service prepends its base
   * path to build a full classpath resource.
   */
  public final String path;

  /**
   * Creates a new sound-effect descriptor.
   *
   * @param path relative filename of the audio asset (e.g., {@code "level-up.wav"})
   */
  SoundCue(final String path) {
    this.path = path;
  }
}
