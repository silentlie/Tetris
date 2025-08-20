package com.example.tetris;

import java.io.IOException;
import javafx.animation.PauseTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class ScenesManager {

  private static volatile ScenesManager instance;
  private Stage primaryStage;

  private ScenesManager() {
  }

  public static synchronized ScenesManager getInstance() {
    if (instance == null) {
      instance = new ScenesManager();
    }
    return instance;
  }

  public void init(Stage primaryStage) {
    if (this.primaryStage == null) {
      this.primaryStage = primaryStage;
    }
  }

  public void goFxml(String fxml) {
    try {
      FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxml));
      // if navigate from a scene, persevere the scene change root only
      if (primaryStage.getScene() != null) {
        primaryStage.getScene().setRoot(loader.load());
      } else {
        // otherwise, create new scene and set size to max
        primaryStage.setScene(new Scene(loader.load()));
        primaryStage.setMaximized(true);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void showSplashScreen(Duration duration, Runnable r) {
    Scene scene = new Scene(new StackPane(new Label("Splash Scene")));
    Stage splashStage = new Stage();
    splashStage.setScene(scene);
    splashStage.setResizable(false);
    splashStage.initStyle(StageStyle.UNDECORATED);
    splashStage.setAlwaysOnTop(true);
    splashStage.show();
    // default delay 15s
    Duration d = duration != null ? duration : Duration.seconds(15);
    PauseTransition delay = new PauseTransition(d);
    if (r != null) {
      delay.setOnFinished(e -> {
        // close splash window
        splashStage.close();
        r.run();
      });
    }
    delay.play();
  }
}
