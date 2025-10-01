package com.example.tetris;

import com.example.tetris.app.context.AppContext;
import com.example.tetris.app.navigation.Route;
import com.example.tetris.app.navigation.Router;
import com.example.tetris.app.navigation.ViewLoader;
import com.example.tetris.service.audio.AudioService;
import com.example.tetris.service.audio.JavaFxAudioService;
import com.example.tetris.service.config.ConfigurationService;
import com.example.tetris.service.config.JsonConfigurationService;
import java.util.Map;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Main entry point for the Tetris JavaFX application.
 *
 * <p>Bootstraps JavaFX, wires core services, and delegates view changes to {@link Router} and
 * {@link ViewLoader}. The initial route may be chosen with the named launch parameter
 * {@code --scene=<name>}.</p>
 *
 * <h2>Launch parameter: {@code --scene}</h2>
 *
 * <p>Supported values (case-sensitive):</p>
 * <ul>
 *   <li>{@code splash} — show only the splash screen (15 s) and then exit</li>
 *   <li>{@code menu} — show the main menu</li>
 *   <li>{@code highScores} — show the high-scores view</li>
 *   <li>{@code configuration} — show the configuration view</li>
 *   <li>{@code play} — start gameplay immediately</li>
 * </ul>
 *
 * <p>If omitted or unrecognized, a short splash (3 s) is shown and then the main menu loads.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>{@link #start(Stage)} is invoked by the JavaFX launcher on the JavaFX application thread.
 * All UI work performed here is therefore on the correct thread.</p>
 *
 * @since 1.0
 */
public class Main extends Application {

  /**
   * Launches the JavaFX application.
   *
   * <p>Accepts an optional {@code --scene=<name>} parameter (see class Javadoc). All other
   * arguments are passed through to JavaFX.</p>
   *
   * @param args command-line arguments; may include {@code --scene=<name>}
   */
  public static void main(String[] args) {
    launch(args);
  }

  /**
   * Configures and displays the primary stage, then navigates to the initial route.
   *
   * <p>Initialization sequence:</p>
   * <ol>
   *   <li>Set the window title.</li>
   *   <li>Create a {@link ViewLoader} and {@link Router} bound to the primary {@link Stage}.</li>
   *   <li>Initialize {@link ConfigurationService} and {@link AudioService}.</li>
   *   <li>Apply the persisted BGM preference to the audio service.</li>
   *   <li>Select the initial scene based on {@code --scene}, or show a 3 s splash then the menu.
   *   </li>
   * </ol>
   *
   * <p>When {@code --scene=splash} is provided, a 15-second splash is shown and the window closes
   * afterward. For other recognized scenes, the route is displayed immediately. Unrecognized values
   * fall back to the short splash then menu.</p>
   *
   * @param primaryStage the main application window created and owned by JavaFX (never
   *                     {@code null})
   */
  @Override
  public void start(Stage primaryStage) {
    // set window title
    primaryStage.setTitle("Tetris");

    // get params from args from main function
    Parameters params = getParameters();
    Map<String, String> paramsNamed = params.getNamed();
    String scene = paramsNamed.get("scene");

    // debug only
    System.out.println(scene);

    ViewLoader loader = new ViewLoader();
    Router router = new Router(primaryStage, loader);

    // Build persistence
    ConfigurationService configurationService = new JsonConfigurationService();
    AudioService audioService = new JavaFxAudioService();
    audioService.setBgm(configurationService.getCache().music());

    // Pass service and cache into AppContext
    AppContext ctx = new AppContext(router, configurationService, audioService);

    switch (scene) {
      case "splash" -> router.showSplash(this.getClass(), null, primaryStage::close);
      case "menu" -> ctx.show(Route.MENU);
      case "highScores" -> ctx.show(Route.HIGH_SCORE);
      case "configuration" -> ctx.show(Route.CONFIGURATION);
      case "play" -> ctx.show(Route.PLAY);
      case null, default -> router.showSplash(
          this.getClass(),
          Duration.seconds(3),
          () -> ctx.show(Route.MENU)
      );
    }
  }
}
