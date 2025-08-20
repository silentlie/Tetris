package com.example.tetris.controllers;

import com.example.tetris.helpers.ScenesManager;
import com.example.tetris.models.Cell;
import com.example.tetris.models.Game;
import com.example.tetris.models.Tetromino;
import com.example.tetris.models.TetrominoType;
import java.util.Optional;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class Play {

  @FXML
  private Pane boardHolder;
  @FXML
  private Canvas boardCanvas;

  private Game game;
  private AnimationTimer timer;
  private boolean dirty = true;
  private boolean isRunning = false;

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
    game = new Game(result.get());
    boardCanvas.setManaged(false);
    boardHolder.layoutBoundsProperty().addListener((obs, o, b) -> {
      boardCanvas.setWidth(b.getWidth());
      boardCanvas.setHeight(b.getHeight());
      render();
    });

    boardHolder.sceneProperty().addListener((obs, oldScene, newScene) -> {
      if (newScene != null) {
        newScene.setOnKeyPressed(event -> {
          switch (event.getCode()) {
            case LEFT -> {
              game.moveLeft();
              dirty = true;
            }
            case RIGHT -> {
              game.moveRight();
              dirty = true;
            }
            case UP -> {
              game.rotate();
              dirty = true;
            }
            case DOWN -> {
              game.moveDown();
              dirty = true;
              if (game.lock()) {
                endGame();
              }
            }
            case SPACE -> {
              while (true) {
                if (!game.moveDown()) {
                  break;
                }
              }
              dirty = true;
              if (game.lock()) {
                endGame();
              }
            }
            case P -> {
              pauseGame();
            }
            default -> {
            }
          }
        });
        // request focus so we actually receive key events
        boardHolder.requestFocus();
      }
    });

    timer = new AnimationTimer() {
      private final long lastTick = 0;
      private final long fallAccNanos = 0L;

      @Override
      public void handle(long now) {
        // TODO: falling tetromino
        render();
      }
    };
  }

  private void endGame() {
    // TODO: end Game and save score
    if (isRunning) {
      isRunning = false;
      timer.stop();
    }

    ScenesManager.getInstance().showMenuScene();
  }

  private void pauseGame() {
    if (!isRunning) {
      return;
    }

    isRunning = false;
    timer.stop();

    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Game Paused");
    alert.setHeaderText("Pause Menu");
    alert.setContentText("What would you like to do?");

    ButtonType leave = new ButtonType("Leave", ButtonBar.ButtonData.OK_DONE);
    ButtonType resume = new ButtonType("Resume", ButtonBar.ButtonData.CANCEL_CLOSE);
    alert.getButtonTypes().setAll(leave, resume);

    ButtonType result = alert.showAndWait().orElse(resume);

    if (result.equals(leave)) {
      endGame();
    } else {
      timer.start();
      isRunning = true;
    }
  }

  private void render() {
    if (!dirty || game == null) {
      return;
    }

    GraphicsContext g = boardCanvas.getGraphicsContext2D();
    double width = boardCanvas.getWidth();
    double height = boardCanvas.getHeight();

    // Clear / background
    g.setFill(Color.web("#111111"));
    g.fillRect(0, 0, width, height);

    int cols = Game.WIDTH;
    int rows = Game.HEIGHT;

    // choose uniform cell size and center the board
    double cell = Math.floor(Math.min(width / cols, height / rows));
    // active overlay
    boolean[][] active = new boolean[rows][cols];
    Tetromino t = game.getTetromino();
    if (t != null) {
      for (Cell c : t.getAbsoluteShape()) {
        int x = c.x();
        int y = c.y();
        if (x >= 0 && x < cols && y >= 0 && y < rows) {
          active[y][x] = true;
        }
      }
    }
    double boardWidth = cell * cols;
    double boardHeight = cell * rows;
    double offsetX = (width - boardWidth) / 2.0;
    double offsetY = (height - boardHeight) / 2.0;
    TetrominoType[][] board = game.getBoard();
    for (int y = 0; y < rows; y++) {
      for (int x = 0; x < cols; x++) {
        // empty
        Color fill = Color.web("#1f1f1f");
        TetrominoType locked = board[y][x];
        if (locked != null) {
          // locked color
          fill = locked.getColor();
        } else if (active[y][x] && t != null) {
          // falling piece color
          fill = t.type().getColor();
        }

        double rx = offsetX + x * cell;
        double ry = offsetY + y * cell;
        g.setFill(fill);
        g.fillRect(rx, ry, cell, cell);

        g.setStroke(Color.web("#2a2a2a"));
        g.strokeRect(rx + 0.5, ry + 0.5, cell - 1.0, cell - 1.0);
      }
    }
    dirty = false;
  }
}
