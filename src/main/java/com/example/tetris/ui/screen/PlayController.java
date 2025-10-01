package com.example.tetris.ui.screen;

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
import com.example.tetris.core.rule.Game.Listener;
import com.example.tetris.core.rule.GameClock;
import com.example.tetris.core.state.GameIntent;
import com.example.tetris.core.state.GameState;
import com.example.tetris.core.state.PieceQueue;
import com.example.tetris.core.state.PlayerType;
import com.example.tetris.service.audio.SoundCue;
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
 * JavaFX controller for the “Play” route.
 *
 * <p>Constructs and drives one or two {@link Game} instances, wires scene-level keyboard input,
 * and manages pause/leave flow.</p>
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *   <li>{@link #setContext(AppContext)} builds games from the current {@link Configuration},
 *       installs scene key handlers, and starts the {@link GameService} game loop.</li>
 *   <li>{@link #pauseGame()} stops the loop and prompts to leave or resume.</li>
 *   <li>{@link #endGame()} stops the loop, shows “Game Over”, navigates home, and evicts this
 *   route.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 *
 * <p>All methods are expected to run on the JavaFX application thread. UI work performed from other
 * threads is marshalled via {@link Platform#runLater(Runnable)}.</p>
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
   * Initialize this controller with the application context and start gameplay.
   *
   * <p>Behavior:</p>
   * <ul>
   *   <li>Reads {@link Configuration} from {@link AppContext#getConfigService()}.</li>
   *   <li>Creates a {@link GameService} that ends the session when all games have finished.</li>
   *   <li>Creates Player 1; if {@code extendMode} is enabled, creates Player 2 and splits the UI.
   *   </li>
   *   <li>Installs a scene-level key handler (see {@link #onKeyPressed(KeyEvent)} for bindings).
   *   </li>
   *   <li>Starts the game loop.</li>
   * </ul>
   *
   * @param ctx application context (must not be {@code null})
   *
   * @throws NullPointerException if {@code ctx} is {@code null}
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

    // --- Player 1 ---
    game1 = createGame(seed, config.width(), config.height(), config.level(), config.player1());
    service.addGame(game1);

    // Install key handler on the active scene
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

  /**
   * Construct a {@link Game} (state + view + clock), attach the view to the UI, and return it.
   *
   * <p>Uses a deterministic piece supplier seeded by {@code seed}. The preview queue length is
   * {@link Configuration#NEXT_QUEUE_SIZE}.</p>
   *
   * @param seed RNG seed for the piece supplier
   *
   * @return a fully wired {@link Game} instance
   */
  private Game createGame(long seed, int width, int height, int startLevel, PlayerType playerType) {
    Supplier<TetrominoType> supplier = PieceSupplierStrategy.UNIFORM.create(seed);
    PieceQueue queue = new PieceQueue(NEXT_QUEUE_SIZE, supplier);
    Board board = new Board(width, height);
    GameState state = new GameState(board, queue);
    GameView view = new GameView();
    root.getItems().add(view);
    GameClock clock = new GameClock(startLevel);
    return game(state, view, clock, playerType);
  }

  /**
   * Wire a {@link Game} with sound cues and return it.
   *
   * <p>Bridges {@link Game.Listener} callbacks to {@link SoundCue}s via the {@link AppContext}’s
   * audio service. No other side effects.</p>
   */
  private Game game(GameState state, GameView view, GameClock clock, PlayerType type) {
    Game game = new Game(state, view, clock);
    switch (type) {
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
    game.setListener(new Listener() {
      @Override
      public void onLevelUp(int newLevel) {
        ctx.getAudio().play(SoundCue.LEVEL_UP);
      }

      @Override
      public void onClearLines(int cleared, int score, int streak, int highestStreak) {
        ctx.getAudio().play(SoundCue.ERASE_LINE);
      }

      @Override
      public void onGameOver() {
        ctx.getAudio().play(SoundCue.GAME_FINISH);
      }

      @Override
      public void onIntentAccepted(GameIntent intent) {
        ctx.getAudio().play(SoundCue.MOVE_TURN);
      }
    });
    return game;
  }

  /**
   * End the session: stop the service, show a “Game Over” dialog, navigate to the menu, and evict
   * this route from the navigation cache.
   *
   * <p>All UI actions run on the FX thread. Safe to call multiple times; stopping an already
   * stopped service is a no-op.</p>
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
   * Pause gameplay and prompt the user to leave or resume.
   *
   * <p>If the {@link GameService} is not running, this call is ignored. Otherwise, the loop is
   * stopped and a modal dialog is shown. Choosing “Leave” calls {@link #endGame()}; choosing
   * “Resume” restarts the loop.</p>
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
   * Scene-level key handler mapping key presses to {@link GameIntent}s for player 1 and, when
   * present, player 2. Also supports toggles and pause.
   *
   * <p><strong>Bindings:</strong></p>
   * <ul>
   *   <li>Pause: {@code P}, {@code ESCAPE}</li>
   *   <li>Player 1: {@code LEFT}/{@code RIGHT}/{@code UP} (rotate CW)/{@code DOWN} (soft drop)</li>
   *   <li>Player 2 (extend mode): {@code ,} (left), {@code .} (right), {@code L} (rotate CW),
   *       {@code SPACE} (soft drop). When Player 2 is absent, these keys control Player 1.</li>
   *   <li>Audio: {@code M} toggle BGM, {@code S} toggle SFX</li>
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

      // Toggles
      case M -> ctx.switchBgm();
      case S -> ctx.switchSound();

      default -> { /* ignore */ }
    }
  }
}
