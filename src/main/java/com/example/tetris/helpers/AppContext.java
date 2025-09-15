package com.example.tetris.helpers;

import com.example.tetris.services.ConfigurationService;

/**
 * Shared application context passed to all controllers.
 *
 * <p>The {@code AppContext} provides access to global services and state
 * for the running application. At minimum, it wraps a {@link Router} so controllers can trigger
 * navigation without directly depending on stage or loader details. It also exposes the
 * {@link ConfigurationService} for accessing and persisting application settings.</p>
 *
 * <p><strong>Typical usage inside a controller:</strong></p>
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
 * <p>This class can be extended in the future to hold additional shared state,
 * such as high scores, user session data, or other services.</p>
 */
public class AppContext {

  private final Router router;
  private final ConfigurationService configurationService;

  /**
   * Creates a new application context bound to the given router and configuration service.
   *
   * @param router               the router responsible for navigation between views
   * @param configurationService service for loading and saving persistent configuration
   */
  public AppContext(Router router, ConfigurationService configurationService) {
    this.router = router;
    this.configurationService = configurationService;
  }

  /**
   * Navigates to the given route using the underlying {@link Router}.
   *
   * @param route the target route to display
   */
  public void show(Route route) {
    router.show(route, this);
  }

  /**
   * Returns the configuration service for reading and writing application settings.
   *
   * @return the configuration service bound to this context
   */
  public ConfigurationService getConfigService() {
    return configurationService;
  }
}
