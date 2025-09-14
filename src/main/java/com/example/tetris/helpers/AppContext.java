package com.example.tetris.helpers;

import com.example.tetris.services.ConfigurationService;

/**
 * Shared application context passed to all controllers.
 *
 * <p>The {@code AppContext} provides access to global services and state
 * for the running application. At minimum, it wraps a {@link Router} so
 * controllers can trigger navigation without directly depending on stage
 * or loader details.</p>
 *
 * <p>Typical usage inside a controller:</p>
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
 *     app.show(Route.PLAY); // navigate via context
 *   }
 * }
 * }</pre>
 *
 * <p>This class can also be extended in the future to hold additional
 * shared state, such as configuration settings, high scores, or user
 * session data.</p>
 */
public class AppContext {
  private final Router router;
  private final ConfigurationService configurationService;
  private final ConfigurationCache configurationCache;

  /**
   * Creates a new application context bound to the given router.
   *
   * @param router the router responsible for navigation
   */
  public AppContext(Router router,
                    ConfigurationService configurationService,
                    ConfigurationCache configurationCache) {

    this.router = router;
    this.configurationService = configurationService;
    this.configurationCache = configurationCache;
  }

  /**
   * Navigates to the given route using the underlying router.
   *
   * @param route the target route to display
   */
  public void show(Route route) {
    router.show(route, this);
  }

  public ConfigurationService getConfigurationService() {
    return configurationService;
  }

  public ConfigurationCache getConfigurationCache() {
    return configurationCache;
  }
}
