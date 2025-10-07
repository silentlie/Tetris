package com.example.tetris.controllers;

import com.example.tetris.helpers.*;
import com.example.tetris.models.Game;
import com.example.tetris.models.HighScore;
import com.example.tetris.models.Tetromino;
import com.example.tetris.models.TetrominoType;
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

import java.util.Optional;

import static com.example.tetris.models.Game.K;

/**
 * Controller class for the Tetris gameplay scene.
 * <p>
 * Handles player input, rendering, scoring, and end-of-game logic.
 */
public class PlayController extends GameTimer implements FxController {

    private AppContext ctx;

    @FXML
    private Pane boardHolder;
    @FXML
    private Canvas boardCanvas;

    private Game game;
    private boolean dirty = true;

    /**
     * Initializes the play scene.
     */
    @FXML
    private void initialize() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Player Name");
        dialog.setHeaderText("Welcome to Tetris!");
        dialog.setContentText("Please enter your name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().trim().isEmpty()) {
            ctx.show(Route.MENU);
            return;
        }
        game = new Game(result.get().trim());

        // Resize canvas dynamically
        boardHolder.layoutBoundsProperty().addListener((obs, o, b) -> {
            boardCanvas.setWidth(b.getWidth());
            boardCanvas.setHeight(b.getHeight());
            markDirty();
            render();
        });

        // Keyboard input
        boardHolder.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) oldScene.setOnKeyPressed(null);
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
     * Main game loop.
     */
    @Override
    protected void gameLoop() {
        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        double level = (1.0 + Math.sqrt(1.0 + (2.0 * elapsedSeconds) / 15.0)) / 2.0;
        double clampedLevel = Math.min(level, K);
        double secondsPerRow = Math.pow(1.0 / 30.0, (clampedLevel - 1.0) / (K - 1));
        long fallIntervalNanos = (long) (secondsPerRow * 1_000_000_000.0);

        while (accumulateNanos >= fallIntervalNanos) {
            accumulateNanos -= fallIntervalNanos;
            if (softDrop()) continue;
            if (lock()) {
                endGame();
                return;
            }
            accumulateNanos = 0L;
        }
        render();
    }

    /**
     * Handles keyboard input.
     */
    private boolean handleKey(KeyCode code) {
        if (game == null) return false;
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
                if (lock()) endGame();
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

    private boolean softDrop() {
        markDirty();
        return game.softDrop();
    }

    private boolean lock() {
        markDirty();
        return game.lock();
    }

    private void markDirty() {
        dirty = true;
    }

    /**
     * Ends the game, saves high score, and shows final message.
     */
    public void endGame() {
        if (isRunning) stop();

        Platform.runLater(() -> {

            HighScoreManager manager = new HighScoreManager();
            manager.addScore(new HighScore(game.getPlayerName(), game.getScore()));

            // Game over alert
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText("Player: " + game.getPlayerName() + "\nYour Score: " + game.getScore());
            alert.setContentText("Thanks for playing, " + game.getPlayerName() + "!");
            alert.showAndWait();

            // Return to main menu (or open High Scores if you prefer)
            ctx.show(Route.MENU);
        });
    }

    /**
     * Pause menu with resume/leave options.
     */
    public void pauseGame() {
        if (!isRunning) return;
        stop();

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
            start();
        }
    }

    /**
     * Draws the board and current tetromino.
     */
    public void render() {
        if (!dirty || game == null) return;

        GraphicsContext g = boardCanvas.getGraphicsContext2D();
        double width = boardCanvas.getWidth();
        double height = boardCanvas.getHeight();

        g.setFill(Color.web("#111111"));
        g.fillRect(0, 0, width, height);

        final int cols = Game.WIDTH;
        final int rows = Game.HEIGHT;
        double cell = Math.floor(Math.min(width / cols, height / rows));
        double boardWidth = cell * cols;
        double boardHeight = cell * rows;
        double offsetX = (width - boardWidth) / 2.0;
        double offsetY = (height - boardHeight) / 2.0;

        TetrominoType[][] board = game.getBoard();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                Color fill = (board[y][x] != null) ? board[y][x].getColor()
                        : Color.web("#1f1f1f");
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

    @Override
    public void setContext(AppContext ctx) {
        this.ctx = ctx;
    }
}
