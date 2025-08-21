package com.example.tetris.helpers;

import com.example.tetris.Main;
import java.io.IOException;
import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Singleton manager responsible for handling scene transitions in the application.
 *
 * <p>This class manages switching between different FXML-based scenes in the primary stage.
 * It also provides utility methods such as showing a splash screen.</p>
 */
public class ScenesManager {

  private static volatile ScenesManager instance;
  private Stage primaryStage;

  private ScenesManager() {
  }

  /**
   * Returns the singleton instance of {@code ScenesManager}.
   *
   * <p>If the instance does not exist yet, it will be created.</p>
   *
   * @return the singleton instance of {@code ScenesManager}
   */
  public static synchronized ScenesManager getInstance() {
    if (instance == null) {
      instance = new ScenesManager();
    }
    return instance;
  }

  /**
   * Initializes the scene manager with the primary stage of the application.
   *
   * <p>This method should be called once at application startup.</p>
   *
   * @param primaryStage the primary stage of the application
   */
  public void init(Stage primaryStage) {
    if (this.primaryStage == null) {
      this.primaryStage = primaryStage;
    }
  }

  /**
   * Loads the specified FXML file and switches the current scene to it.
   *
   * <p>If the primary stage already has a scene, only the root node is replaced
   * to preserve stage configuration. Otherwise, a new scene is created and maximized to fill the
   * window.</p>
   *
   * @param fxml the name of the FXML file to load (must be in the classpath)
   *
   * @throws RuntimeException if the FXML file cannot be loaded
   */
  private void goFxml(String fxml) {
    try {
      FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxml));
      System.out.println(Main.class.getResource(fxml));
      if (primaryStage.getScene() != null) {
        primaryStage.getScene().setRoot(loader.load());
      } else {
        primaryStage.setScene(new Scene(loader.load()));
        primaryStage.setMaximized(true);
        primaryStage.show();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Displays a splash screen for the given duration, then executes the provided {@link Runnable}.
   *
   * <p>If the duration is {@code null}, the splash screen will be displayed for 15 seconds by
   * default.</p>
   *
   * @param duration the time to show the splash screen, or {@code null} for default (15s)
   * @param r        a callback to run after the splash screen closes; may be {@code null}
   */
  public void showSplashScreen(Duration duration, Runnable r) {
    String url = Objects.requireNonNull(Main.class.getResource("splash.png")).toString();
    ImageView splashImage = new ImageView(new Image(url));
    splashImage.setPreserveRatio(true);
    Scene scene = new Scene(new StackPane(splashImage));
    Stage splashStage = new Stage();
    splashStage.initOwner(primaryStage);
    splashStage.setScene(scene);
    splashStage.setResizable(false);
    splashStage.initStyle(StageStyle.UNDECORATED);
    splashStage.setAlwaysOnTop(true);
    splashStage.show();
    Duration d = duration != null ? duration : Duration.seconds(15);
    PauseTransition delay = new PauseTransition(d);
    delay.setOnFinished(e -> {
      splashStage.close();
      if (r != null) {
        r.run();
      }
    });
    delay.play();
  }

  /**
   * Shows the main menu scene.
   */
  public void showMenuScene() {
    goFxml("menu.fxml");
  }

  /**
   * Shows the high score scene.
   */
  public void showHighScoreScene() {
    goFxml("high-score.fxml");
  }

  /**
   * Shows the settings scene.
   */
  public void showConfigurationScene() {
    goFxml("configuration.fxml");
  }

  /**
   * Shows the gameplay scene.
   */
  public void showPlayScene() {
    goFxml("play.fxml");
  }
}
