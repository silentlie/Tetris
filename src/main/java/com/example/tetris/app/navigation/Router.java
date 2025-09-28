package com.example.tetris.app.navigation;

import com.example.tetris.app.context.AppContext;
import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Handles navigation between application views and manages the primary stage.
 *
 * <p>The {@code Router} acts as a central navigation service: it loads views
 * defined in {@link Route} using a {@link ViewLoader} and swaps them into the primary
 * {@link Stage}. It also provides a utility method to display a temporary splash screen before
 * navigating to another route or performing a custom action.</p>
 *
 * <p><strong>Threading:</strong> {@link #show(Route, AppContext)} is thread-safe and will
 * delegate to the JavaFX application thread as needed. Other methods should be called on the FX
 * thread unless documented otherwise.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ViewLoader loader = new ViewLoader(Main.class);
 * Router router = new Router(primaryStage, loader);
 * AppContext ctx = new AppContext(router);
 *
 * // Show a splash screen for 3 seconds, then the menu:
 * router.showSplash(Main.class, Duration.seconds(3), () -> ctx.show(Route.MENU));
 *
 * // Navigate directly to the gameplay views:
 * ctx.show(Route.PLAY);
 * }</pre>
 */
public class Router {

  private final Stage stage;
  private final ViewLoader loader;

  /**
   * Creates a new router bound to the given stage and views loader.
   *
   * @param stage  the primary stage of the application
   * @param loader the loader used to resolve {@link Route} definitions
   */
  public Router(Stage stage, ViewLoader loader) {
    this.stage = stage;
    this.loader = loader;
  }

  /**
   * Returns the primary {@link Stage} managed by this router.
   *
   * <p>Useful for setting modality/owner on dialogs, adjusting window properties
   * (icon, title), or querying the current {@link Scene}. Be aware that mutating the returned stage
   * affects the whole app.</p>
   *
   * @return the primary stage (never {@code null})
   */
  public Stage getStage() {
    return stage;
  }

  /**
   * Displays the views associated with the given route.
   *
   * <p>If the stage has no scene yet, a new scene is created and maximized.
   * If a scene already exists, its root node is replaced while preserving stage settings.</p>
   *
   * <p><strong>Threading:</strong> Safe to call from any thread; will dispatch to
   * the FX application thread if necessary.</p>
   *
   * @param route   the views to display
   * @param context the shared {@link AppContext} to inject into the controller
   */
  public void show(Route route, AppContext context) {
    if (!Platform.isFxApplicationThread()) {
      Platform.runLater(() -> show(route, context));
      return;
    }
    Parent root = loader.load(route, context);
    if (stage.getScene() == null) {
      stage.setScene(new Scene(root));
      stage.setMaximized(true);
      stage.show();
    } else {
      stage.getScene().setRoot(root);
    }
  }

  /**
   * Removes any cached view/controller associated with the given route from the
   * {@link ViewLoader}.
   *
   * <p>Use this to force a fresh load the next time {@link #show(Route, AppContext)} is called for
   * the same route (for example, after configuration changes or to release memory).</p>
   *
   * @param route the route to evict from cache
   */
  public void evict(Route route) {
    loader.evict(route);
  }

  /**
   * Shows a temporary splash screen image.
   *
   * <p>The splash is displayed in a borderless, always-on-top stage owned by the
   * primary stage. After the given duration elapses, the splash is closed and the optional callback
   * is invoked.</p>
   *
   * <p>If {@code duration} is {@code null}, the splash defaults to 15 seconds.</p>
   *
   * <p><strong>Threading:</strong> Must be called on the JavaFX application thread.</p>
   *
   * @param resourceAnchor a class whose classloader and package are used to resolve
   *                       {@code splash.png} (typically {@code Main.class})
   * @param duration       how long the splash remains visible; defaults to 15s if {@code null}
   * @param after          action to run after the splash closes; may be {@code null}
   *
   * @throws NullPointerException if {@code splash.png} cannot be resolved relative to
   *                              {@code resourceAnchor}
   */
  public void showSplash(Class<?> resourceAnchor, Duration duration, Runnable after) {
    String url = Objects.requireNonNull(
        resourceAnchor.getResource("images/splash.png"),
        "Missing splash.png"
    ).toExternalForm();

    ImageView splashImage = new ImageView(new Image(url));
    splashImage.setPreserveRatio(true);

    Scene splashScene = new Scene(new StackPane(splashImage));
    Stage splashStage = new Stage(StageStyle.UNDECORATED);
    splashStage.initOwner(stage);
    splashStage.setAlwaysOnTop(true);
    splashStage.setScene(splashScene);
    splashStage.show();

    Duration d = (duration != null) ? duration : Duration.seconds(15);
    PauseTransition delay = new PauseTransition(d);
    delay.setOnFinished(e -> {
      splashStage.close();
      if (after != null) {
        after.run();
      }
    });
    delay.play();
  }
}
