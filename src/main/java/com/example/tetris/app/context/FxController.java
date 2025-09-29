package com.example.tetris.app.context;

import com.example.tetris.app.navigation.ViewLoader;

/**
 * Common base interface for all JavaFX FXML controller in the Tetris application.
 *
 * <p>Each controller is provided with an {@link AppContext} after its FXML has been
 * loaded. This allows controller to access shared application state and perform navigation without
 * relying on static singletons.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * public final class MenuController implements FxController {
 *   private AppContext ctx;
 *
 *   @Override
 *   public void setContext(AppContext ctx) {
 *     this.ctx = ctx;
 *   }
 *
 *   @FXML
 *   private void onPlayButtonClick(ActionEvent actionEvent) {
 *     ctx.show(Route.PLAY);
 *   }
 * }
 * }</pre>
 */
public interface FxController {

  /**
   * Supplies the shared {@link AppContext} to this controller.
   *
   * <p>Called automatically by {@link ViewLoader} after the FXML file has been
   * loaded. Implementations should store the context for later use.</p>
   *
   * @param ctx the application context for navigation and shared state
   */
  void setContext(AppContext ctx);
}
