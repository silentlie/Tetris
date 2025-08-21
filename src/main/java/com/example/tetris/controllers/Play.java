package com.example.tetris.controllers;

import com.example.tetris.helpers.GameLoopCallbacks;
import com.example.tetris.helpers.GameTimer;
import com.example.tetris.helpers.ScenesManager;
import com.example.tetris.models.Game;
import com.example.tetris.models.Tetromino;
import com.example.tetris.models.TetrominoType;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * Controller class for the Tetris game play scene.
 *
 * <p>Handles user input, rendering the board and active tetromino,
 * and manages game loop callbacks such as dropping and locking pieces.
 * </p>
 */
public class Play implements GameLoopCallbacks {

  @FXML
  private Pane boardHolder;
  @FXML
  private Canvas boardCanvas;

  private Game game;
  private GameTimer timer;

  private boolean dirty = true;

  /**
   * Initializes the play scene.
   *
   * <p>Prompts for player name, sets up canvas resizing, scene key handling, and starts the
   * game timer.
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
      ScenesManager.getInstance().showMenuScene();
      return;
    }
    game = new Game(result.get().trim());

    // Resize canvas with parent and trigger a redraw.
    boardHolder.layoutBoundsProperty().addListener((obs, o, b) -> {
      boardCanvas.setWidth(b.getWidth());
      boardCanvas.setHeight(b.getHeight());
      markDirty();
      render();
    });

    boardHolder.sceneProperty().addListener((obs, oldScene, newScene) -> {
      if (oldScene != null) {
        oldScene.setOnKeyPressed(null);
      }
      if (newScene != null) {
        newScene.setOnKeyPressed(event -> {
          if (handleKey(event.getCode())) {
            event.consume();
          }
        });
      }
    });

    timer = new GameTimer(this);
    timer.start();
  }

  /**
   * Handles key input for controlling the game.
   *
   * @param code the {@link KeyCode} pressed by the player
   *
   * @return true if the key was handled, false otherwise
   */
  private boolean handleKey(KeyCode code) {
    if (game == null) {
      return false;
    }
    switch (code) {
      case LEFT -> {
        game.moveLeft();
        markDirty();
        return true;
      }
      case RIGHT -> {
        game.moveRight();
        markDirty();
        return true;
      }
      case UP -> {
        game.rotate();
        markDirty();
        return true;
      }
      case DOWN -> {
        if (softDrop()) {
          return true;
        }
        if (lock()) {
          endGame();
        }
        return true;
      }
      case SPACE -> {
        boolean moved;
        do {
          moved = softDrop();
        } while (moved);
        if (lock()) {
          endGame();
        }
        return true;
      }
      case P -> {
        pauseGame();
        return true;
      }
      default -> {
        return false;
      }
    }
  }

  /**
   * Attempts to soft drop the current tetromino (move down one cell).
   *
   * @return true if the tetromino moved down, false if blocked
   */
  @Override
  public boolean softDrop() {
    markDirty();
    return game.softDrop();
  }

  /**
   * Locks the current tetromino into the board if it cannot move further.
   *
   * @return true if the game should end, false otherwise
   */
  @Override
  public boolean lock() {
    markDirty();
    return game.lock();
  }

  /**
   * Ends the game, stopping the timer and returning to the menu scene.
   */
  @Override
  public void endGame() {
    if (timer != null && timer.isRunning()) {
      timer.stop();
    }
    // TODO: Alert Score and save Score
    ScenesManager.getInstance().showMenuScene();
  }

  /**
   * Marks the canvas as dirty, requiring a redraw on the next render call.
   */
  @Override
  public void markDirty() {
    dirty = true;
  }

  /**
   * Pauses the game and shows a confirmation dialog. Player can either resume or leave the game.
   */
  public void pauseGame() {
    if (timer == null || !timer.isRunning()) {
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
   * Renders the game board and active tetromino onto the canvas.
   *
   * <p>Only redraws if the {@code dirty} flag is set.
   * </p>
   */
  @Override
  public void render() {
    if (!dirty || game == null) {
      return;
    }

    GraphicsContext g = boardCanvas.getGraphicsContext2D();
    double width = boardCanvas.getWidth();
    double height = boardCanvas.getHeight();

    // Background
    g.setFill(Color.web("#111111"));
    g.fillRect(0, 0, width, height);

    final int cols = Game.WIDTH;
    final int rows = Game.HEIGHT;

    // Compute uniform cell size and board offset (centre the board)
    double cell = Math.floor(Math.min(width / cols, height / rows));
    double boardWidth = cell * cols;
    double boardHeight = cell * rows;
    double offsetX = (width - boardWidth) / 2.0;
    double offsetY = (height - boardHeight) / 2.0;
    TetrominoType[][] board = game.getBoard();
    for (int y = 0; y < rows; y++) {
      for (int x = 0; x < cols; x++) {
        Color fill = (board[y][x] != null) ? board[y][x].getColor()
            : javafx.scene.paint.Color.web("#1f1f1f");
        double rx = offsetX + x * cell;
        double ry = offsetY + y * cell;
        g.setFill(fill);
        g.fillRect(rx, ry, cell, cell);

        g.setStroke(Color.web("#2a2a2a"));
        g.strokeRect(rx, ry, cell, cell);
      }
    }

    Tetromino t = game.getTetromino();
    if (t != null) {
      g.setFill(t.type().getColor());
      for (var c : t.getAbsoluteShape()) {
        int x = c.x();
        int y = c.y();
        double rx = offsetX + x * cell;
        double ry = offsetY + y * cell;
        g.fillRect(rx, ry, cell, cell);
        g.setStroke(Color.web("#2a2a2a"));
        g.strokeRect(rx, ry, cell, cell);
      }
    }

    dirty = false;
  }
}