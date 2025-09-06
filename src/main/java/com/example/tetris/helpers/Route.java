package com.example.tetris.helpers;

import com.example.tetris.controllers.ConfigurationController;
import com.example.tetris.controllers.HighScoreController;
import com.example.tetris.controllers.MenuController;
import com.example.tetris.controllers.PlayController;

/**
 * Enumeration of all navigable views (routes) in the Tetris application.
 *
 * <p>Each enum constant defines the FXML resource to load and the expected
 * controller type associated with that view. This provides a type-safe way to
 * reference screens without relying on raw string paths.</p>
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

  /** Main menu screen. */
  MENU("menu.fxml", MenuController.class),

  /** High score listing screen. */
  HIGH_SCORE("high-score.fxml", HighScoreController.class),

  /** Configuration/settings screen. */
  CONFIGURATION("configuration.fxml", ConfigurationController.class),

  /** Gameplay screen. */
  PLAY("play.fxml", PlayController.class);

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