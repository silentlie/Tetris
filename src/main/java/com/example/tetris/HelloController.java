package com.example.tetris;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * The type Hello controller.
 */
public class HelloController {

  @FXML
  private Label welcomeText;

  /**
   * On the hello button click.
   */
  @FXML
  protected void onHelloButtonClick() {

    welcomeText.setText("Welcome to JavaFX Application!");
  }
}