package com.example.tetris.controllers;

import com.example.tetris.helpers.ScenesManager;
import com.example.tetris.models.Game;
import java.util.Optional;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Pane;

public class Play {

  @FXML
  private Pane boardHolder;
  @FXML
  private Canvas boardCanvas;

  private Game game;
  private AnimationTimer timer;
  private final boolean isRunning = false;

  @FXML
  private void initialize() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Player Name");
    dialog.setHeaderText("Welcome to the Game!");
    dialog.setContentText("Please enter your name:");
    Optional<String> result = dialog.showAndWait();
    if (result.isEmpty() || result.get().trim().isEmpty()) {
      Platform.runLater(ScenesManager.getInstance()::showMenuScene);
      return;
    }

    boardCanvas.setManaged(false);
    boardHolder.layoutBoundsProperty().addListener((obs, o, b) -> {
      boardCanvas.setWidth(b.getWidth());
      boardCanvas.setHeight(b.getHeight());
      render();
    });
    timer = new AnimationTimer() {
      private final long fallAccNanos = 0L;

      @Override
      public void handle(long now) {
        // TODO:

      }
    };
  }

  private void render() {

  }
}
