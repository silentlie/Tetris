package com.example.tetris;

import java.io.IOException;
import java.util.Map;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws IOException {
    // set window title
    primaryStage.setTitle("Tetris");
    Parameters params = getParameters();
    Map<String, String> paramsNamed = params.getNamed();
    String scene = paramsNamed.get("scene");
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
    } else if ("settings".equals(scene)) {
      sceneManager.showSettingsScene();
    } else if ("play".equals(scene)) {
      sceneManager.showPlayScene();
    }
  }
}
