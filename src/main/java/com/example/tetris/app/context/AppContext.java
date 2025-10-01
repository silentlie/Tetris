package com.example.tetris.app.context;

import com.example.tetris.app.navigation.Route;
import com.example.tetris.app.navigation.Router;
import com.example.tetris.service.audio.AudioService;
import com.example.tetris.service.config.ConfigurationService;
import java.util.Objects;
import javafx.stage.Stage;

/**
 * Shared application context passed to all JavaFX controllers.
 *
 * <p>{@code AppContext} centralizes access to cross-cutting services and app-level state:
 * navigation via a {@link Router}, configuration persistence via a {@link ConfigurationService},
 * and audio playback via an {@link AudioService}. Controllers receive this context (typically
 * through an {@code FxController#setContext(AppContext)}) and invoke it to perform navigation or
 * update global settings without reaching into the primary {@link Stage} or loader internals.</p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Expose the primary {@link Stage} for dialog ownership and window metadata.</li>
 *   <li>Navigate between routes, passing the same context through.</li>
 *   <li>Evict route caches to force fresh loads when needed.</li>
 *   <li>Provide access to configuration and audio services.</li>
 *   <li>Offer convenience toggles for music/SFX that update both config and runtime audio.</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 *
 * <p>Public methods that cause UI changes delegate to the {@link Router}, which is responsible
 * for dispatching to the JavaFX application thread as needed. This class itself holds only final
 * references and is effectively immutable.</p>
 *
 * <h2>Typical usage inside a controller</h2>
 * <pre>{@code
 * public final class MenuController implements FxController {
 *   private AppContext app;
 *
 *   @Override
 *   public void setContext(AppContext ctx) {
 *     this.app = ctx;
 *   }
 *
 *   @FXML
 *   private void onPlayClicked() {
 *     app.show(Route.PLAY);                 // navigate via context
 *     app.getAudio().play(SoundCue.CLICK);  // play feedback sound
 *   }
 * }
 * }</pre>
 *
 * <p>This class may be extended in the future to aggregate additional services such as
 * high-scores, user sessions, telemetry, or i18n resources.</p>
 *
 * @since 1.0
 */
public class AppContext {

  private final Router router;
  private final ConfigurationService configurationService;
  private final AudioService audioService;

  /**
   * Creates a new application context bound to the given router, configuration service, and audio
   * service.
   *
   * @param router               the router responsible for navigation between views (must not be
   *                             {@code null})
   * @param configurationService service for loading and saving persistent configuration (must not
   *                             be {@code null})
   * @param audioService         service for playing music and sound effects (must not be
   *                             {@code null})
   *
   * @throws NullPointerException if any argument is {@code null}
   */
  public AppContext(Router router,
      ConfigurationService configurationService,
      AudioService audioService) {
    this.router = Objects.requireNonNull(router, "router");
    this.configurationService = Objects.requireNonNull(configurationService,
        "configurationService");
    this.audioService = Objects.requireNonNull(audioService, "audioService");
  }

  /**
   * Returns the primary {@link Stage} managed by the underlying {@link Router}.
   *
   * <p>Use this for dialog ownership/modality (e.g., {@code alert.initOwner(getStage())}),
   * window title/icon updates, or querying the current scene. Mutating the returned stage affects
   * the whole application.</p>
   *
   * @return the primary stage (never {@code null})
   */
  public Stage getStage() {
    return router.getStage();
  }

  /**
   * Navigates to the given route using the underlying {@link Router}.
   *
   * <p>Safe to call from any thread; the router will marshal to the JavaFX application thread as
   * needed. The current {@code AppContext} is supplied to the target controller.</p>
   *
   * @param route the target route to display (must not be {@code null})
   *
   * @throws NullPointerException if {@code route} is {@code null}
   */
  public void show(Route route) {
    router.show(Objects.requireNonNull(route, "route"), this);
  }

  /**
   * Evicts any cached view/controller for the given route from the router’s loader.
   *
   * <p>Useful to force a fresh load on next navigation (for example, after configuration changes
   * or to release memory).</p>
   *
   * @param route the route to evict (must not be {@code null})
   *
   * @throws NullPointerException if {@code route} is {@code null}
   */
  public void evict(Route route) {
    router.evict(Objects.requireNonNull(route, "route"));
  }

  /**
   * Returns the configuration service for reading and writing application settings.
   *
   * @return the configuration service bound to this context (never {@code null})
   */
  public ConfigurationService getConfigService() {
    return configurationService;
  }

  /**
   * Returns the audio service for playing background music and sound effects.
   *
   * @return the audio service bound to this context (never {@code null})
   */
  public AudioService getAudio() {
    return audioService;
  }

  /**
   * Toggle the background-music setting and apply it immediately.
   *
   * <p>This method:
   * <ol>
   *   <li>Reads the current cached {@code Configuration}.</li>
   *   <li>Flips the {@code music} flag and persists the new configuration via
   *   {@link ConfigurationService#save}.</li>
   *   <li>Propagates the new state to the audio system via {@link AudioService#setBgm(boolean)}.
   *   </li>
   * </ol>
   *
   * <p>Effect is immediate for both persistence and runtime audio. UI-thread dispatch, if required,
   * is handled by underlying services.</p>
   */
  public void switchBgm() {
    boolean enabled = !configurationService.getCache().music();
    configurationService.save(configurationService.getCache().withMusic(enabled));
    audioService.setBgm(enabled);
  }

  /**
   * Toggle the sound-effects setting and apply it immediately.
   *
   * <p>This method:
   * <ol>
   *   <li>Reads the current cached {@code Configuration}.</li>
   *   <li>Flips the {@code sound} flag and persists the new configuration via
   *   {@link ConfigurationService#save}.</li>
   *   <li>Propagates the new state to the audio system via {@link AudioService#setSound(boolean)}.
   *   </li>
   * </ol>
   *
   * <p>Effect is immediate for both persistence and runtime audio. UI-thread dispatch, if required,
   * is handled by underlying services.</p>
   */
  public void switchSound() {
    boolean enabled = !configurationService.getCache().sound();
    configurationService.save(configurationService.getCache().withSound(enabled));
    audioService.setSound(enabled);
  }
}
