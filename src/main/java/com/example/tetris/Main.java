package com.example.tetris;

import com.example.tetris.helpers.ScenesManager;
import java.io.IOException;
import java.util.Map;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Entry point for the Tetris application.
 *
 * <p>This class initializes JavaFX, configures the primary stage, and delegates scene
 * management to {@link ScenesManager}. The initial scene can be selected using named launch
 * parameters (e.g., {@code --scene=menu}).</p>
 */
public class Main extends Application {

  /**
   * Launches the JavaFX Tetris application.
   *
   * @param args command-line arguments; may include {@code --scene=<sceneName>} to select the
   *             initial scene. Supported values are:
   *             <ul>
   *               <li>{@code splash} – shows only the splash screen</li>
   *               <li>{@code menu} – shows the main menu</li>
   *               <li>{@code highScores} – shows the high scores scene</li>
   *               <li>{@code configuration} – shows the configuration scene</li>
   *               <li>{@code play} – shows the gameplay scene</li>
   *             </ul>
   */
  public static void main(String[] args) {
    launch(args);
  }

  /**
   * Initializes and displays the primary stage of the application.
   *
   * <p>The startup behavior depends on the {@code --scene} launch parameter:</p>
   * <ul>
   *   <li>No parameter – shows a splash screen for 3 seconds, then the main menu.</li>
   *   <li>{@code splash} – shows the splash screen for 15 seconds, then closes the application.
   *   </li>
   *   <li>{@code menu} – shows the main menu scene.</li>
   *   <li>{@code highScores} – shows the high scores scene.</li>
   *   <li>{@code configuration} – shows the configuration scene.</li>
   *   <li>{@code play} – shows the gameplay scene.</li>
   * </ul>
   *
   * @param primaryStage the primary stage provided by the JavaFX runtime
   * @throws IOException if a required FXML file cannot be loaded
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
    ScenesManager sceneManager = ScenesManager.getInstance();
    sceneManager.init(primaryStage);
    if (scene == null) {
      sceneManager.showSplashScreen(Duration.seconds(3), sceneManager::showMenuScene);
    } else if ("splash".equals(scene)) {
      sceneManager.showSplashScreen(null, primaryStage::close);
    } else if ("menu".equals(scene)) {
      sceneManager.showMenuScene();
    } else if ("highScores".equals(scene)) {
      sceneManager.showHighScoresScene();
    } else if ("configuration".equals(scene)) {
      sceneManager.showConfigurationScene();
    } else if ("play".equals(scene)) {
      sceneManager.showPlayScene();
    }
  }
}
