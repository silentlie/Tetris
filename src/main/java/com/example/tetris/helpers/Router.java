package com.example.tetris.helpers;

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
 * defined in {@link Route} using a {@link ViewLoader} and swaps them into the
 * primary {@link Stage}. It also provides a utility method to display a
 * temporary splash screen before navigating to another route or performing
 * a custom action.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ViewLoader loader = new ViewLoader(Main.class);
 * Router router = new Router(primaryStage, loader);
 * AppContext ctx = new AppContext(router);
 *
 * // Show a splash screen for 3 seconds, then the menu:
 * router.showSplash(Duration.seconds(3), () -> ctx.show(Route.MENU));
 *
 * // Navigate directly to the gameplay view:
 * ctx.show(Route.PLAY);
 * }</pre>
 */
public class Router {
  private final Stage stage;
  private final ViewLoader loader;

  /**
   * Creates a new router bound to the given stage and view loader.
   *
   * @param stage  the primary stage of the application
   * @param loader the loader used to resolve {@link Route} definitions
   */
  public Router(Stage stage, ViewLoader loader) {
    this.stage = stage;
    this.loader = loader;
  }

  /**
   * Displays the view associated with the given route.
   *
   * <p>If the stage has no scene yet, a new scene is created and maximized.
   * If a scene already exists, its root node is simply replaced while
   * preserving stage settings.</p>
   *
   * <p>This method is thread-safe and will delegate to the JavaFX
   * application thread if called from a background thread.</p>
   *
   * @param route   the view to display
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
   * Shows a temporary splash screen image.
   *
   * <p>The splash is displayed in a borderless, always-on-top stage. After the
   * given duration elapses, the splash is closed and the optional callback
   * is invoked.</p>
   *
   * <p>If {@code duration} is {@code null}, the splash defaults to 15 seconds.</p>
   *
   * @param duration how long the splash should remain visible; defaults to 15s if {@code null}
   * @param after    action to run after the splash closes; may be {@code null}
   */
  public void showSplash(Duration duration, Runnable after) {
    String url = Objects.requireNonNull(
        stage.getClass().getResource("splash.png"),
        "Missing splash.png").toExternalForm();

    ImageView splashImage = new ImageView(new Image(url));
    splashImage.setPreserveRatio(true);

    Scene splashScene = new Scene(new StackPane(splashImage));
    Stage splashStage = new Stage(StageStyle.UNDECORATED);
    splashStage.initOwner(stage);
    splashStage.setAlwaysOnTop(true);
    splashStage.setScene(splashScene);
    splashStage.show();

    Duration d = duration != null ? duration : Duration.seconds(15);
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
