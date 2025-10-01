package com.example.tetris.service.audio;

/**
 * Facade for game audio playback: background music (BGM) and short sound effects (SFX).
 *
 * <p>Implementations resolve audio assets (e.g., JavaFX {@code MediaPlayer}/{@code AudioClip} or
 * another backend), honor user preferences (enable/disable), and manage the lifecycle of players,
 * clips, and any worker threads.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * audio.setBgm(true);                  // enable & loop background music
 * audio.setSound(true);                // enable SFX
 * audio.play(SoundCue.LINE_CLEAR);     // play a one-shot effect
 * }</pre>
 *
 * <h2>Behavioral guarantees</h2>
 * <ul>
 *   <li><strong>Idempotent:</strong> Repeating the same call yields no adverse effects
 *       (e.g., calling {@code setBgm(true)} twice does not stack loops).</li>
 *   <li><strong>Non-blocking:</strong> Calls return promptly; playback is asynchronous.</li>
 *   <li><strong>Threading:</strong> Unless stated otherwise, invoke from the JavaFX application
 *       thread. Implementations may internally marshal as needed.</li>
 *   <li><strong>Muted semantics:</strong> When disabled via {@link #setBgm(boolean)} or
 *       {@link #setSound(boolean)}, corresponding operations become no-ops.</li>
 * </ul>
 *
 * @since 1.0
 */
public interface AudioService extends AutoCloseable {

  /**
   * Enable or disable background music (BGM).
   *
   * <p>When {@code enabled} is {@code true}, the implementation should begin or resume looping
   * playback of the current background track. When {@code false}, it should stop playback
   * promptly.</p>
   *
   * <p>Calling this method with the current state is a no-op.</p>
   *
   * @param enabled {@code true} to activate looping BGM; {@code false} to stop
   */
  void setBgm(boolean enabled);

  /**
   * Enable or disable sound effects (SFX).
   *
   * <p>When {@code enabled} is {@code true}, subsequent {@link #play(SoundCue)} calls should be
   * audible. When {@code false}, {@code play(...)} should return immediately without producing
   * sound.</p>
   *
   * <p>Calling this method with the current state is a no-op.</p>
   *
   * @param enabled {@code true} to allow SFX playback; {@code false} to mute SFX
   */
  void setSound(boolean enabled);

  /**
   * Play the sound effect associated with the given cue.
   *
   * <p>Implementations should resolve the cue to a preloaded/ready clip and play it asynchronously
   * at the configured volume. If SFX are disabled via {@link #setSound(boolean)}, this method
   * should be a no-op.</p>
   *
   * @param event semantic identifier of the effect to play (must not be {@code null})
   *
   * @throws NullPointerException if {@code event} is {@code null}
   */
  void play(SoundCue event);

  /**
   * Release all audio resources (players, clips, threads).
   *
   * <p>After {@code close()}, the service should be considered terminated and must not be used
   * again. This method is idempotent and may be called multiple times safely.</p>
   *
   * @throws Exception if cleanup fails (implementations may elect not to throw)
   */
  @Override
  void close() throws Exception;
}
