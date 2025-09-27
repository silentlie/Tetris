package com.example.tetris.controllers;

import com.example.tetris.helpers.AppContext;
import com.example.tetris.helpers.FxController;
import com.example.tetris.helpers.GameService;
import com.example.tetris.helpers.Route;
import com.example.tetris.models.Board;
import com.example.tetris.models.Configuration;
import com.example.tetris.models.Game;
import com.example.tetris.models.GameClock;
import com.example.tetris.models.GameIntent;
import com.example.tetris.models.GameState;
import com.example.tetris.models.PieceQueue;
import com.example.tetris.models.PieceSupplierStrategy;
import com.example.tetris.models.TetrominoType;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyEvent;

public class PlayController implements FxController {

  @FXML
  private SplitPane root;

  private AppContext ctx;
  private GameService service;
  private Game game1;
  private Game game2;

  @Override
  public void setContext(AppContext ctx) {
    this.ctx = ctx;
    // --- Config & deterministic RNGs ---
    Configuration config = ctx.getConfigService().getCache();
    service = new GameService(this::endGame);
    long seed = System.nanoTime();
    // TODO: AI control if config.player1() == PlayerType.AI or config.player2() == PlayerType.AI
    // TODO: connect to server at port 3000 if config.player1() == PlayerType.EXTERNAL or
    //  config.player2() == PlayerType.EXTERNAL
    game1 = createGame(seed, config);
    service.addGame(game1);

    // --- Player 2 (extend mode) ---
    if (config.extendMode()) {
      game2 = createGame(seed, config);
      service.addGame(game2);
      root.setDividerPositions(0.5);
    }
    root.sceneProperty().addListener((obs, oldScene, newScene) -> {
      if (oldScene != null) {
        oldScene.setOnKeyPressed(null);
      }
      if (newScene != null) {
        newScene.setOnKeyPressed(this::onKeyPressed);
      }
    });
    service.start();
  }

  private Game createGame(long seed, Configuration config) {
    Supplier<TetrominoType> supplier = PieceSupplierStrategy.UNIFORM.create(seed);
    PieceQueue queue = new PieceQueue(3, supplier);
    Board board = new Board(config.width(), config.height());
    GameState state = new GameState(board, queue);
    GameView view = new GameView();
    root.getItems().add(view);
    GameClock clock = new GameClock(config.level());
    return new Game(state, view, clock);
  }

  public void endGame() {
    if (service != null && service.isRunning()) {
      service.stop();
    }
    Platform.runLater(() -> {
      // TODO: ask name for each player and save high scores
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Game Over");
      alert.setHeaderText(null);
      alert.setContentText("Nice run!");
      alert.showAndWait();
      ctx.show(Route.MENU);
      ctx.evict(Route.PLAY);
    });
  }

  public void pauseGame() {
    if (service == null || !service.isRunning()) {
      return;
    }
    service.stop();
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Game Paused");
    alert.setHeaderText("Paused");
    alert.setContentText("What would you like to do?");

    ButtonType leave = new ButtonType("Leave", ButtonBar.ButtonData.OK_DONE);
    ButtonType resume = new ButtonType("Resume", ButtonBar.ButtonData.CANCEL_CLOSE);
    alert.getButtonTypes().setAll(leave, resume);

    ButtonType result = alert.showAndWait().orElse(resume);
    if (result.equals(leave)) {
      endGame();
    } else {
      service.start();
    }
  }

  private void onKeyPressed(KeyEvent keyEvent) {
    keyEvent.consume();
    switch (keyEvent.getCode()) {
      case P, ESCAPE -> pauseGame();
      // Player 1
      case LEFT -> game1.apply(GameIntent.MOVE_LEFT);
      case RIGHT -> game1.apply(GameIntent.MOVE_RIGHT);
      case UP -> game1.apply(GameIntent.ROTATE_CW);
      case DOWN -> game1.apply(GameIntent.SOFT_DROP);
      case COMMA -> {
        if (game2 != null) {
          game2.apply(GameIntent.MOVE_LEFT);
        } else {
          game1.apply(GameIntent.MOVE_LEFT);
        }
      }
      // Player 2
      case PERIOD -> {
        if (game2 != null) {
          game2.apply(GameIntent.MOVE_RIGHT);
        } else {
          game1.apply(GameIntent.MOVE_RIGHT);
        }
      }
      case L -> {
        if (game2 != null) {
          game2.apply(GameIntent.ROTATE_CW);
        } else {
          game1.apply(GameIntent.ROTATE_CW);
        }
      }
      case SPACE -> {
        if (game2 != null) {
          game2.apply(GameIntent.SOFT_DROP);
        } else {
          game1.apply(GameIntent.SOFT_DROP);
        }
      }
      default -> { /* ignore */ }
    }
  }
}
