package com.example.tetris;

import com.example.tetris.app.context.AppContext;
import com.example.tetris.app.navigation.Route;
import com.example.tetris.app.navigation.Router;
import com.example.tetris.app.navigation.ViewLoader;
import com.example.tetris.service.audio.AudioService;
import com.example.tetris.service.audio.JavaFxAudioService;
import com.example.tetris.service.config.ConfigurationService;
import com.example.tetris.service.config.JsonConfigurationService;
import com.example.tetris.service.scores.JsonScoresService;
import com.example.tetris.service.scores.ScoresService;
import java.util.Map;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Main entry point for the Tetris JavaFX application.
 *
 * <p>Bootstraps JavaFX, wires core services, and delegates view changes to {@link Router} and
 * {@link ViewLoader}. The initial route may be selected via the named launch parameter
 * {@code --scene=<name>}.</p>
 *
 * <h2>Launch parameter: {@code --scene}</h2>
 *
 * <p>Supported case-sensitive values:</p>
 * <ul>
 *   <li>{@code splash} — show only the splash screen (15&nbsp;s) and then exit</li>
 *   <li>{@code menu} — show the main menu</li>
 *   <li>{@code highScores} — show the high-scores view</li>
 *   <li>{@code configuration} — show the configuration view</li>
 *   <li>{@code play} — start gameplay immediately</li>
 * </ul>
 *
 * <p>If omitted or unrecognized, a short splash (3&nbsp;s) is shown and then the main menu loads.
 * </p>
 *
 * <h2>Lifecycle & threading</h2>
 *
 * <p>{@link #start(Stage)} is invoked by the JavaFX launcher on the JavaFX Application Thread.
 * All UI work performed here runs on that thread. Shutdown occurs when the primary stage is closed,
 * or immediately after the 15&nbsp;s splash if {@code --scene=splash} is used.</p>
 *
 * <h2>Services</h2>
 *
 * <p>At startup, this class constructs:</p>
 * <ul>
 *   <li>{@link ConfigurationService} — JSON-backed configuration with cached access.</li>
 *   <li>{@link ScoresService} — JSON-backed leaderboard.</li>
 *   <li>{@link AudioService} — JavaFX-based audio; background music (BGM) is enabled/disabled
 *       according to the persisted configuration.</li>
 * </ul>
 *
 * @see Application
 * @see Router
 * @see ViewLoader
 * @see Route
 * @see ConfigurationService
 * @see ScoresService
 * @see AudioService
 * @since 1.0
 */
public class Main extends Application {

  /**
   * Launches the JavaFX application.
   *
   * <p>Accepts an optional {@code --scene=<name>} parameter (see class Javadoc). All other
   * arguments are passed through to JavaFX.</p>
   *
   * <p>Example:</p>
   * <pre>{@code
   * java -jar tetris.jar --scene=play
   * }</pre>
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
   *   <li>Initialize {@link ConfigurationService}, {@link ScoresService}, and {@link AudioService}.
   *   </li>
   *   <li>Apply the persisted BGM preference to the audio service.</li>
   *   <li>Select the initial scene based on {@code --scene}, or show a 3&nbsp;s splash then the
   *   menu.</li>
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
    ScoresService scoresService = new JsonScoresService();
    AudioService audioService = new JavaFxAudioService();
    audioService.setBgm(configurationService.getCache().music());

    // Pass service and cache into AppContext
    AppContext ctx = new AppContext(router, configurationService, audioService, scoresService);

    switch (scene) {
      case "splash" -> router.showSplash(this.getClass(), null, primaryStage::close);
      case "menu" -> ctx.show(Route.MENU);
      case "highScores" -> ctx.show(Route.HIGH_SCORES);
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
