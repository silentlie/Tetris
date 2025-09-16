package com.example.tetris.controllers;

import static com.example.tetris.models.Configuration.MAX_LEVEL;

import com.example.tetris.helpers.AppContext;
import com.example.tetris.helpers.FxController;
import com.example.tetris.helpers.GameTimer;
import com.example.tetris.helpers.Route;
import com.example.tetris.models.Configuration;
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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * Controller class for the Tetris gameplay scene.
 *
 * <p>Handles player input, rendering the board and active tetromino, and drives the game loop
 * (gravity, soft drop, locking pieces).
 * </p>
 */
public class PlayController extends GameTimer implements FxController {

  private AppContext ctx;

  @FXML
  private Pane boardHolder;
  @FXML
  private Canvas boardCanvas;

  private Game game;
  private boolean dirty = true;
  private double initialTimeSeconds;

  @Override
  public void setContext(AppContext ctx) {
    this.ctx = ctx;
    int startLevel = ctx.getConfigService().getCache().level();
    initialTimeSeconds = 30.0 * (startLevel * startLevel - startLevel);
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Player Name");
    dialog.setHeaderText("Welcome to the Game!");
    dialog.setContentText("Please enter your name:");

    Optional<String> result = dialog.showAndWait();
    while (result.isEmpty() || result.get().trim().isEmpty()) {
      result = dialog.showAndWait();
    }
    long seed = System.nanoTime();
    Configuration config = ctx.getConfigService().getCache();
    game = new Game(result.get().trim(), seed, config.width(), config.height());

    // Resize canvas dynamically and trigger redraw.
    boardHolder.layoutBoundsProperty().addListener((obs, o, b) -> {
      boardCanvas.setWidth(b.getWidth());
      boardCanvas.setHeight(b.getHeight());
      markDirty();
      render();
    });

    // Register keyboard input when scene becomes active.
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
    start();
  }

  /**
   * Main game loop, invoked by {@link GameTimer}.
   *
   * <p>Advances game state by applying gravity based on elapsed time and player level. If a piece
   * can no longer fall, it locks and the game ends when no new piece can spawn.
   * </p>
   */
  @Override
  protected void gameLoop() {
    double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
    double totalTime = initialTimeSeconds + elapsedSeconds;
    double level = (1.0 + Math.sqrt(1.0 + (2.0 * totalTime) / 15.0)) / 2.0;
    double clampedLevel = Math.min(level, MAX_LEVEL);
    double secondsPerRow = Math.pow(1.0 / 30.0, (clampedLevel - 1.0) / (MAX_LEVEL - 1));
    long fallIntervalNanos = (long) (secondsPerRow * 1_000_000_000.0);

    while (accumulateNanos >= fallIntervalNanos) {
      accumulateNanos -= fallIntervalNanos;
      if (softDrop()) {
        continue;
      }
      if (lock()) {
        endGame();
        return;
      }
      accumulateNanos = 0L;
    }
    render();
  }

  /**
   * Handles keyboard input for controlling tetromino and game state.
   *
   * <ul>
   *   <li>{@code LEFT}  → Move piece left</li>
   *   <li>{@code RIGHT} → Move piece right</li>
   *   <li>{@code UP}    → Rotate piece</li>
   *   <li>{@code DOWN}  → Soft drop (move down faster)</li>
   *   <li>{@code SPACE} → Hard drop (instant drop to bottom)</li>
   *   <li>{@code P}     → Pause game</li>
   * </ul>
   *
   * @param code the {@link KeyCode} pressed by the player
   * @return true if the key was handled, false otherwise
   */
  private boolean handleKey(KeyCode code) {
    if (game == null) {
      return false;
    }
    switch (code) {
      case LEFT -> {
        moveLeft();
        return true;
      }
      case RIGHT -> {
        moveRight();
        return true;
      }
      case UP -> {
        rotate();
        return true;
      }
      case DOWN -> {
        softDrop();
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

  private void rotate() {
    markDirty();
    game.rotate();
  }

  private void moveRight() {
    markDirty();
    game.moveRight();
  }

  private void moveLeft() {
    markDirty();
    game.moveLeft();
  }

  /**
   * Attempts to move the active piece down by one row.
   *
   * @return true if the piece moved down, false if blocked
   */
  private boolean softDrop() {
    markDirty();
    return game.softDrop();
  }

  /**
   * Attempts to lock the current piece in place.
   *
   * @return true if the piece locked and no new piece could spawn (game over)
   */
  private boolean lock() {
    markDirty();
    return game.lock();
  }

  private void markDirty() {
    dirty = true;
  }

  /**
   * Ends the game, stops the timer, shows the final score, and returns to the main menu scene.
   */
  public void endGame() {
    if (isRunning) {
      stop();
    }
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Game Over");
      alert.setHeaderText("Player: " + game.getPlayerName() + "\nYour Score: " + game.getScore());
      alert.setContentText("Thanks for playing, " + game.getPlayerName() + "!");

      alert.showAndWait();
      ctx.show(Route.MENU);
      ctx.evict(Route.PLAY);
    });
  }

  /**
   * Pauses the game and shows a confirmation dialog.
   *
   * <p>The player can choose to resume play or leave the game, returning to the main menu.
   * </p>
   */
  public void pauseGame() {
    if (!isRunning) {
      return;
    }
    stop();

    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Game Paused");
    alert.setHeaderText("Pause MenuController");
    alert.setContentText("What would you like to do?");

    ButtonType leave = new ButtonType("Leave", ButtonBar.ButtonData.OK_DONE);
    ButtonType resume = new ButtonType("Resume", ButtonBar.ButtonData.CANCEL_CLOSE);
    alert.getButtonTypes().setAll(leave, resume);

    ButtonType result = alert.showAndWait().orElse(resume);
    if (result.equals(leave)) {
      endGame();
    } else {
      start();
    }
  }

  /**
   * Renders the game board and the active tetromino onto the canvas.
   *
   * <p>Redraw is performed only when the {@code dirty} flag is set. This avoids unnecessary
   * rendering work.
   * </p>
   */
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

    final int cols = game.width;
    final int rows = game.height;

    // Compute uniform cell size and board offset (center the board).
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
