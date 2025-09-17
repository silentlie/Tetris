package com.example.tetris;

import com.example.tetris.helpers.AppContext;
import com.example.tetris.helpers.ConfigurationCache;
import com.example.tetris.helpers.Route;
import com.example.tetris.helpers.Router;
import com.example.tetris.helpers.ViewLoader;
import com.example.tetris.models.Configuration;
import com.example.tetris.services.ConfigurationService;
import com.example.tetris.services.JsonConfigurationService;
import com.example.tetris.services.AudioService;
import com.example.tetris.services.JavaFxAudioService;
import java.io.IOException;
import java.util.Map;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Duration;


/**
 * Main entry point of the Tetris JavaFX application.
 *
 * <p>This class bootstraps JavaFX, sets up the primary stage, and delegates
 * view navigation to {@link Router} and {@link ViewLoader}. The initial screen
 * can be chosen with a named launch parameter (e.g. {@code --scene=menu}).</p>
 */
public class Main extends Application {

  /**
   * Starts the Tetris application.
   *
   * <p>Accepts an optional named parameter {@code --scene=<name>} to control
   * which scene is shown at startup. Supported values:</p>
   * <ul>
   *   <li>{@code splash} – show only the splash screen (15 s) and then exit</li>
   *   <li>{@code menu} – show the main menu</li>
   *   <li>{@code highScores} – show the high scores view</li>
   *   <li>{@code configuration} – show the configuration view</li>
   *   <li>{@code play} – start gameplay directly</li>
   * </ul>
   *
   * @param args command-line arguments, may include {@code --scene=<name>}
   */
  public static void main(String[] args) {
    launch(args);
  }

  // Defines AppContext as ctx.
  private AppContext ctx;

  /**
   * Configures and displays the primary stage.
   *
   * <p>If no {@code --scene} parameter is provided, the application shows a
   * splash screen for three seconds and then switches to the main menu.</p>
   *
   * <p>If {@code --scene} is specified, behavior is as follows:</p>
   * <ul>
   *   <li>{@code splash} – show the splash screen for 15 s and then close</li>
   *   <li>{@code menu} – show the main menu</li>
   *   <li>{@code highScores} – show the high scores view</li>
   *   <li>{@code configuration} – show the configuration view</li>
   *   <li>{@code play} – show the gameplay view</li>
   *   <li>other values – ignored (no scene shown)</li>
   * </ul>
   *
   * @param primaryStage the main application window created by JavaFX
   * @throws IOException if loading of an FXML resource fails
   */
  @Override
  public void start(Stage primaryStage) throws IOException {
    // set window title
    primaryStage.setTitle("Tetris");
    // get params from args from main function
    Parameters params = getParameters();
    Map<String, String> paramsNamed = params.getNamed();
    String scene = paramsNamed.get("scene");
    // debug only
    System.out.println(scene);
    ViewLoader loader = new ViewLoader(Main.class);
    Router router = new Router(primaryStage, loader);

    // Build persistence
    ConfigurationService configService = new JsonConfigurationService();
    ConfigurationCache configCache = new ConfigurationCache();

    // --------------- Audio --------------------------------
    // Create implementation and pass service into AppContext
    AudioService audio = new JavaFxAudioService();
    ctx = new AppContext(router, configService, configCache, audio);

    // Get audio from persisted configuration
    // Read once from the cache
    // Mirror flags into the audio service, then start music if its enabled.
    Configuration cfg = ctx.getConfigurationCache().getOrLoad(ctx.getConfigurationService());
    ctx.getAudioService().setMusicEnabled(cfg.music());
    ctx.getAudioService().setSoundEnabled(cfg.sound());

    if (cfg.music()) {
      ctx.getAudioService().startMusic();
    }

    // Pause / resume music when the app is minimised
    primaryStage.iconifiedProperty().addListener((obs, was, iconified) -> {
      if (iconified) {
        ctx.getAudioService().stopMusic();
      } else {
        // read latest
        boolean musicOn = ctx.getConfigurationCache().getOrLoad(ctx.getConfigurationService()).music();
        if (musicOn) {
          ctx.getAudioService().startMusic();
        }
      }
    });

    // ------------------------------------------------------------

    if (null == scene) {
      router.showSplash(Main.class, Duration.seconds(3), () -> ctx.show(Route.MENU));
    } else {
      switch (scene) {
        case "splash" -> router.showSplash(Main.class, null, primaryStage::close);
        case "menu" -> ctx.show(Route.MENU);
        case "highScores" -> ctx.show(Route.HIGH_SCORE);
        case "configuration" -> ctx.show(Route.CONFIGURATION);
        case "play" -> ctx.show(Route.PLAY);
        default -> {
        }
      }
    }
  }

  // Audio
  // For clean shutdown of audio resources.
  @Override
  public void stop() {
    // Ensures media player/clips are released when the app exits.
    if (ctx != null && ctx.getAudioService() != null) {
      ctx.getAudioService().dispose();
    }
  }
}
