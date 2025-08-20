package com.example.tetris.controllers;

import com.example.tetris.helpers.ScenesManager;
import com.example.tetris.models.Cell;
import com.example.tetris.models.Game;
import com.example.tetris.models.Tetromino;
import com.example.tetris.models.TetrominoType;
import java.util.Optional;
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

/**
 * Controller class for the Play scene of the game.
 *
 * <p>Handles player input, rendering of the board, managing game state
 * (start, pause, end), and integrating with the {@link Game} and {@link GameTimer}.
 * </p>
 */
public class Play {

  @FXML
  private Pane boardHolder;
  @FXML
  private Canvas boardCanvas;

  private Game game;
  private GameTimer timer;
  private boolean dirty = true;

  /**
   * Initializes the Play scene.
   *
   * <p>Prompts for player name, sets up listeners for resizing and keyboard input,
   * and starts the game timer.
   * </p>
   */
  @FXML
  private void initialize() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Player Name");
    dialog.setHeaderText("Welcome to the Game!");
    dialog.setContentText("Please enter your name:");
    Optional<String> result = dialog.showAndWait();
    if (result.isEmpty() || result.get().trim().isEmpty()) {
      // No player name provided → return to menu
      Platform.runLater(ScenesManager.getInstance()::showMenuScene);
      return;
    }
    game = new Game(result.get());
    // Adjust canvas size to match boardHolder
    boardHolder.layoutBoundsProperty().addListener((obs, o, b) -> {
      boardCanvas.setWidth(b.getWidth());
      boardCanvas.setHeight(b.getHeight());
      dirty = true;
      render();
    });
    // Handle keyboard input for controlling the game
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
              if (!game.moveDown()) {
                if (game.lock()) {
                  endGame();
                }
              }
              dirty = true;
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
            case P -> pauseGame();
            default -> {
              // ignore other keys
            }
          }
        });
      }
    });

    timer = new GameTimer() {
      @Override
      protected boolean onFallStep() {
        return game.moveDown();
      }

      @Override
      protected boolean onLock() {
        return game.lock();
      }

      @Override
      protected void onEndGame() {
        endGame();
      }

      @Override
      protected void onRender() {
        render();
      }

      @Override
      protected void onDirty() {
        dirty = true;
      }
    };
    timer.start();
  }

  /**
   * Ends the current game session.
   *
   * <p>Stops the timer, and switches back to the menu scene.
   * </p>
   */
  private void endGame() {
    // TODO: end Game and save score
    if (timer.isRunning()) {
      timer.stop();
    }

    ScenesManager.getInstance().showMenuScene();
  }

  /**
   * Pauses the game and shows a dialog with options to resume or leave the game.
   */
  private void pauseGame() {
    if (!timer.isRunning()) {
      return;
    }
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
    }
  }

  /**
   * Renders the current state of the game board onto the canvas.
   *
   * <p>Draws locked pieces, the active falling tetromino, and
   * background/grid styling.
   * </p>
   */
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
