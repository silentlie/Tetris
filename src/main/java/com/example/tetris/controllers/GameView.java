package com.example.tetris.controllers;

import com.example.tetris.Main;
import com.example.tetris.helpers.BoardCanvas;
import com.example.tetris.helpers.NextCanvas;
import com.example.tetris.models.GameState;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.paint.Color;

public class GameView extends SplitPane {

  public static final Color GRID = Color.web("#2a2a2a");
  public static final Color BACKGROUND = Color.web("#111111");
  public static final Color EMPTY = Color.web("#1f1f1f");

  @FXML
  private Label labelInitialLevel;
  @FXML
  private Label labelCurrentLevel;
  @FXML
  private Label labelScore;
  @FXML
  private Label labelStreak;
  @FXML
  private BoardCanvas boardCanvas;
  @FXML
  private NextCanvas nextCanvas;

  private boolean dirty = false;

  public GameView() {
    FXMLLoader fxml = new FXMLLoader(Main.class.getResource("game-view.fxml"));
    fxml.setRoot(this);
    fxml.setController(this);
    try {
      fxml.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load GameView.fxml", e);
    }
  }

  @FXML
  private void initialize() {
    widthProperty().addListener((o, oldV, newV) -> markDirty());
    heightProperty().addListener((o, oldV, newV) -> markDirty());
  }

  @Override
  protected void layoutChildren() {
    super.layoutChildren();
    markDirty();
  }

  public void markDirty() {
    dirty = true;
    if (boardCanvas != null) {
      boardCanvas.markDirty();
    }
    if (nextCanvas != null) {
      nextCanvas.markDirty();
    }
  }


  public void render(GameState s, int initialLevel, int currentLevel) {

    labelInitialLevel.setText("Initial Level: " + initialLevel);
    labelCurrentLevel.setText("Current Level: " + currentLevel);
    labelScore.setText("Score: " + s.getScore());
    labelStreak.setText("Streak: " + s.getStreak());
    if (!dirty) {
      return;
    }
    if (boardCanvas != null) {
      System.out.println(currentLevel);
      boardCanvas.render(s);
    }
    if (nextCanvas != null) {
      nextCanvas.render(s.getQueue());
    }
    dirty = false;
  }
}
