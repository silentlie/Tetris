package com.example.tetris.app.navigation;

import com.example.tetris.app.context.FxController;
import com.example.tetris.ui.controller.ConfigurationController;
import com.example.tetris.ui.controller.HighScoreController;
import com.example.tetris.ui.controller.MenuController;
import com.example.tetris.ui.controller.PlayController;

/**
 * Enumeration of all navigable views (routes) in the Tetris application.
 *
 * <p>Each enum constant defines the FXML resource to load and the expected
 * controller type associated with that views. This provides a type-safe way to reference screens
 * without relying on raw string paths.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * // Navigate to the play screen:
 * ctx.show(Route.PLAY);
 *
 * // Load the controller type explicitly:
 * Class<? extends FxController> type = Route.MENU.controllerType();
 * }</pre>
 */
public enum Route {

  /**
   * Main menu screen.
   */
  MENU("screens/menu.fxml", MenuController.class),

  /**
   * High score listing screen.
   */
  HIGH_SCORE("screens/high-score.fxml", HighScoreController.class),

  /**
   * Configuration/settings screen.
   */
  CONFIGURATION("screens/configuration.fxml", ConfigurationController.class),

  /**
   * Gameplay screen.
   */
  PLAY("screens/play.fxml", PlayController.class);

  private final String fxml;
  private final Class<? extends FxController> controllerType;

  Route(String fxml, Class<? extends FxController> controllerType) {
    this.fxml = fxml;
    this.controllerType = controllerType;
  }

  /**
   * Returns the FXML resource path for this route.
   *
   * @return the relative FXML filename (resolved via {@code getResource})
   */
  public String fxml() {
    return fxml;
  }

  /**
   * Returns the expected controller class for this route.
   *
   * @return the controller type bound to the FXML file
   */
  public Class<? extends FxController> controllerType() {
    return controllerType;
  }
}