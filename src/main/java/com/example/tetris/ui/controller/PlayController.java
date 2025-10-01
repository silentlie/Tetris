package com.example.tetris.ui.controller;

import static com.example.tetris.core.config.Configuration.NEXT_QUEUE_SIZE;

import com.example.tetris.ai.Ai;
import com.example.tetris.ai.EternalAi;
import com.example.tetris.ai.FakeAi;
import com.example.tetris.app.context.AppContext;
import com.example.tetris.app.context.FxController;
import com.example.tetris.app.navigation.Route;
import com.example.tetris.app.runtime.GameService;
import com.example.tetris.core.board.Board;
import com.example.tetris.core.config.Configuration;
import com.example.tetris.core.piece.TetrominoType;
import com.example.tetris.core.random.PieceSupplierStrategy;
import com.example.tetris.core.rule.Game;
import com.example.tetris.core.rule.GameClock;
import com.example.tetris.core.state.GameIntent;
import com.example.tetris.core.state.GameState;
import com.example.tetris.core.state.PieceQueue;
import com.example.tetris.core.state.PlayerType;
import com.example.tetris.ui.view.GameView;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyEvent;

/**
 * JavaFX controller for the “Play” route. Constructs and drives one or two {@link Game} instances,
 * wires keyboard input, and manages pause/leave flow.
 *
 * <p>Lifecycle:</p>
 * <ul>
 *   <li>{@link #setContext(AppContext)} builds games from configuration, attaches key handlers,
 *       and starts the {@link GameService} loop.</li>
 *   <li>{@link #pauseGame()} temporarily stops the loop and shows a modal dialog to leave or
 *   resume.</li>
 *   <li>{@link #endGame()} stops the loop, shows “Game Over”, and navigates back to the menu.</li>
 * </ul>
 *
 * <p>Threading: all methods are expected to run on the JavaFX application thread.</p>
 *
 * @since 1.0
 */
public class PlayController implements FxController {

  @FXML
  private SplitPane root;

  private AppContext ctx;
  private GameService service;
  private Game game1;
  private Game game2;

  /**
   * Initializes this controller with the application context and starts gameplay.
   *
   * <p>Behavior:</p>
   * <ul>
   *   <li>Loads {@link Configuration} from the context.</li>
   *   <li>Creates a {@link GameService} that ends the session when all games are finished.</li>
   *   <li>Creates Player 1, and Player 2 if {@code extendMode} is enabled, then starts the loop.
   *   </li>
   *   <li>Installs a scene-level key handler ({@link #onKeyPressed(KeyEvent)}).</li>
   * </ul>
   *
   * @param ctx application context (must not be {@code null})
   */
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

    if (config.extendMode()) {
      game2 = createGame(seed, config.width(), config.height(), config.level(), config.player2());
      service.addGame(game2);
      root.setDividerPositions(0.5);
    }

    game1 = createGame(seed, config.width(), config.height(), config.level(), config.player1());
    service.addGame(game1);

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

  private Game createGame(long seed, int width, int height, int startLevel, PlayerType playerType) {
    Supplier<TetrominoType> supplier = PieceSupplierStrategy.UNIFORM.create(seed);
    PieceQueue queue = new PieceQueue(NEXT_QUEUE_SIZE, supplier);
    Board board = new Board(width, height);
    GameState state = new GameState(board, queue);
    GameView view = new GameView();
    root.getItems().add(view);
    GameClock clock = new GameClock(startLevel);
    Game game = new Game(state, view, clock);
    switch (playerType) {
      case AI -> {
        Ai ai = new FakeAi();
        game.ai(ai);
      }
      case EXTERNAL -> {
        Ai ai = new EternalAi();
        game.ai(ai);
      }
      default -> {
      }
    }
    return game;
  }

  /**
   * Ends the session: stops the service, shows a “Game Over” dialog, navigates home, and evicts
   * this route from the navigation cache.
   */
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

  /**
   * Pauses gameplay and prompts the user to leave or resume.
   *
   * <p>If the service is not running, the call is ignored. Otherwise, the loop is stopped and a
   * modal dialog is shown. Choosing “Leave” calls {@link #endGame()}, choosing “Resume” restarts
   * the loop.</p>
   */
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

  /**
   * Scene-level key handler that maps key presses to {@link GameIntent}s for player 1 (and player 2
   * when present). Also supports pause via {@code P} or {@code ESCAPE}.
   *
   * <p>Controls:</p>
   * <ul>
   *   <li>Pause: {@code P}, {@code ESCAPE}</li>
   *   <li>Player 1: {@code LEFT}/{@code RIGHT}/{@code UP}/{@code DOWN}</li>
   *   <li>Player 2 (extend mode): {@code ,} (left), {@code .} (right), {@code L} (rotate CW),
   *       {@code SPACE} (soft drop). If player 2 is not active, these keys control player 1.</li>
   * </ul>
   *
   * @param keyEvent incoming key event; always consumed
   */
  private void onKeyPressed(KeyEvent keyEvent) {
    keyEvent.consume();
    switch (keyEvent.getCode()) {
      case P, ESCAPE -> pauseGame();

      // Player 1
      case LEFT -> game1.apply(GameIntent.MOVE_LEFT);
      case RIGHT -> game1.apply(GameIntent.MOVE_RIGHT);
      case UP -> game1.apply(GameIntent.ROTATE_CW);
      case DOWN -> game1.apply(GameIntent.SOFT_DROP);

      // Player 2 (or fallback to P1 if second game not present)
      case COMMA -> {
        if (game2 != null) {
          game2.apply(GameIntent.MOVE_LEFT);
        } else {
          game1.apply(GameIntent.MOVE_LEFT);
        }
      }
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
