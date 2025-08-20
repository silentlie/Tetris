package com.example.tetris;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws IOException {
    primaryStage.setTitle("Tetris");
    FXMLLoader loader = new FXMLLoader(Main.class.getResource("menu.fxml"));
    Scene scene = new Scene(loader.load());
    primaryStage.setScene(scene);
    primaryStage.show();
  }
}
