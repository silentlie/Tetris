package com.example.tetris.app.navigation;

import com.example.tetris.app.context.AppContext;
import com.example.tetris.app.context.FxController;
import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/**
 * Utility responsible for loading and caching FXML-based views.
 *
 * <p>This class resolves {@link Route} definitions into their corresponding
 * {@link Parent} node hierarchies and initializes their controller with the shared
 * {@link AppContext}. Loaded views are cached so that repeated navigation to the same route does
 * not reparse the FXML file or create a new controller instance.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ViewLoader loader = new ViewLoader(Main.class);
 * Router router = new Router(primaryStage, loader);
 * AppContext ctx = new AppContext(router);
 *
 * // Load the main menu once and reuse it on every navigation:
 * Parent menuRoot = loader.load(Route.MENU, ctx);
 * }</pre>
 */
public final class ViewLoader {

  private final Map<Route, Parent> roots = new EnumMap<>(Route.class);

  /**
   * Creates a new loader.
   */
  public ViewLoader() {
  }

  /**
   * Loads the views for the given route, initializing its controller with the supplied
   * {@link AppContext}.
   *
   * <p>If the views have already been loaded previously, the cached instance is
   * returned instead of parsing the FXML again.</p>
   *
   * @param route   the route to load
   * @param context the shared application context to inject into the controller
   *
   * @return the root node of the loaded scene graph
   *
   * @throws RuntimeException if the FXML file is missing or cannot be loaded
   */
  public Parent load(Route route, AppContext context) {
    assert Platform.isFxApplicationThread() : "ViewLoader.load must run on FX thread";
    Parent cached = roots.get(route);
    if (cached != null) {
      return cached;
    }

    try {
      URL url = Objects.requireNonNull(route.controllerType().getResource(route.fxml()),
          "FXML not found on classpath: " + route.fxml());
      FXMLLoader loader = new FXMLLoader(url);
      Parent root = loader.load();

      FxController controller = route.controllerType().cast(loader.getController());
      controller.setContext(context);

      roots.put(route, root);
      return root;

    } catch (IOException e) {
      throw new RuntimeException("Failed to load " + route.fxml(), e);
    }
  }

  /**
   * Reloads the views for the given route, discarding any cached instance.
   *
   * @param route   the route to reload
   * @param context the shared application context to inject into the new controller
   *
   * @return the newly loaded root node
   */
  public Parent reload(Route route, AppContext context) {
    evict(route);
    return load(route, context);
  }

  /**
   * Removes the cached instance of the given route.
   *
   * @param route the route to evict
   */
  public void evict(Route route) {
    roots.remove(route);
  }

  /**
   * Removes all cached views.
   */
  public void evictAll() {
    roots.clear();
  }
}
