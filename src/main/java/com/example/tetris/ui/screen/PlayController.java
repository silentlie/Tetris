package com.example.tetris.ui.screen;

import static com.example.tetris.core.config.Configuration.NEXT_QUEUE_SIZE;

import com.example.tetris.ai.Ai;
import com.example.tetris.ai.ExternalAi;
import com.example.tetris.ai.HeuristicAi;
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
import com.example.tetris.core.score.Score;
import com.example.tetris.core.state.GameIntent;
import com.example.tetris.core.state.GameState;
import com.example.tetris.core.state.PieceQueue;
import com.example.tetris.core.state.PlayerType;
import com.example.tetris.service.audio.SoundCue;
import com.example.tetris.ui.view.GameView;
import com.example.tetris.util.TimeUtil;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyEvent;
import org.jetbrains.annotations.NotNull;

/**
 * JavaFX controller for the “Play” route.
 *
 * <p>Constructs and drives one or two {@link Game} instances, wires scene-level keyboard input,
 * manages pause/leave flow, and persists scores at the end of a session.</p>
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *   <li>{@link #setContext(AppContext)} builds games from the current {@link Configuration},
 *       installs scene key handlers, and starts the {@link GameService} loop.</li>
 *   <li>{@link #pauseGame()} stops the loop and prompts to leave or resume.</li>
 *   <li>{@link #endGame()} stops the loop, saves score(s), shows placement info, navigates home,
 *       and evicts this route.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 *
 * <p>All controller methods are expected to run on the JavaFX Application Thread. Any UI work
 * initiated from non-FX threads is marshalled via {@link Platform#runLater(Runnable)}.</p>
 *
 * <h2>Input</h2>
 *
 * <p>Scene-level key bindings are installed when the controller’s root is attached to a scene.
 * See {@link #onKeyPressed(KeyEvent)} for details.</p>
 *
 * @see Game
 * @see GameClock
 * @see GameService
 * @see AppContext
 * @see Route
 * @since 1.0
 */
public class PlayController implements FxController {

  @FXML
  private SplitPane root;

  private AppContext ctx;
  private GameService service;
  private Instant now;
  private Game game1;
  private Game game2;

  /**
   * Initialize this controller with the application context and start gameplay.
   *
   * <p><strong>Behavior:</strong></p>
   * <ul>
   *   <li>Reads {@link Configuration} from {@link AppContext#getConfigService()}.</li>
   *   <li>Creates a {@link GameService} that invokes {@link #endGame()} once all games finish.</li>
   *   <li>Creates Player&nbsp;1; if {@code extendMode} is enabled, also creates Player&nbsp;2 and
   *   splits the UI.</li>
   *   <li>Installs a scene-level key handler (see {@link #onKeyPressed(KeyEvent)}).</li>
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
    now = Instant.now();

    if (config.extendMode()) {
      game2 = createGame(
          now.toEpochMilli(),
          config.width(),
          config.height(),
          config.level(),
          config.player2()
      );
      root.setDividerPositions(0.5);
    }

    // --- Player 1 ---
    game1 = createGame(
        now.toEpochMilli(),
        config.width(),
        config.height(),
        config.level(),
        config.player1()
    );

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
   * Construct a {@link Game} (state + view + clock), attach the view to the UI, add the game to the
   * {@link GameService}, and return it.
   *
   * <p>Uses a deterministic {@link TetrominoType} supplier seeded by {@code seed}. The preview
   * queue length is {@link Configuration#NEXT_QUEUE_SIZE}.</p>
   *
   * @param seed       RNG seed for the piece supplier
   * @param width      board width in cells
   * @param height     board height in cells
   * @param startLevel initial level for the {@link GameClock}
   * @param playerType how this player is controlled (human, built-in AI, or external)
   *
   * @return a fully wired {@link Game} instance
   *
   * @see PieceSupplierStrategy
   * @see GameClock
   * @see GameView
   */
  private Game createGame(
      long seed,
      int width,
      int height,
      int startLevel,
      PlayerType playerType
  ) {
    Supplier<TetrominoType> supplier = PieceSupplierStrategy.UNIFORM.create(seed);
    GameState state = getGameState(width, height, supplier);
    GameView view = new GameView();
    root.getItems().add(view);
    GameClock clock = new GameClock(startLevel);
    clock.setListener(new GameClock.Listener() {
      @Override
      public void onLevelUp(int newLevel) {
        ctx.getAudio().play(SoundCue.LEVEL_UP);
      }
    });
    Game game = new Game(state, view, clock);
    switch (playerType) {
      // TODO: Real AI
      case AI -> {
        Ai ai = new HeuristicAi();
        game.ai(ai);
      }
      case EXTERNAL -> {
        Ai ai = new ExternalAi();
        game.ai(ai);
      }
      default -> {
        // HUMAN — no AI attached
      }
    }
    service.addGame(game);
    return game;
  }

  /**
   * Creates a fresh {@link GameState} (board + queue) and installs sound-producing listeners.
   *
   * @param width    board width in cells
   * @param height   board height in cells
   * @param supplier tetromino supplier providing randomized pieces
   *
   * @return a non-null, ready-to-play {@link GameState}
   */
  private @NotNull GameState getGameState(int width, int height, Supplier<TetrominoType> supplier) {
    PieceQueue queue = new PieceQueue(NEXT_QUEUE_SIZE, supplier);
    Board board = new Board(width, height);
    GameState state = new GameState(board, queue);
    state.setListener(new GameState.Listener() {
      @Override
      public void onClearLines(int cleared, int score, int streak, int highestStreak) {
        ctx.getAudio().play(SoundCue.ERASE_LINE);
      }

      @Override
      public void onGameOver() {
        ctx.getAudio().play(SoundCue.GAME_FINISH);
      }

      @Override
      public void onIntent(GameIntent intent) {
        ctx.getAudio().play(SoundCue.MOVE_TURN);
      }
    });
    return state;
  }

  /**
   * End the session: stop the service, prompt for player name(s), persist score(s), show placement,
   * navigate to the menu, and evict the play route.
   *
   * <p>All UI actions run on the FX thread. Calling this method multiple times is safe; stopping
   * an already-stopped service is a no-op.</p>
   */
  public void endGame() {
    if (service != null && service.isRunning()) {
      service.stop();
    }
    Platform.runLater(() -> {
      if (ctx.getConfigService().getCache().extendMode()) {
        Score score1 = promptAndSaveScore(
            game1.score(),
            game1.highestStreak(),
            game1.elapsed()
        );
        Score score2 = promptAndSaveScore(
            game2.score(),
            game2.highestStreak(),
            game2.elapsed()
        );
        int[] ranks = ctx.getScoresService().addAll(List.of(score1, score2));
        new Alert(
            Alert.AlertType.INFORMATION,
            "Placements:\n• " + score1.name() + " — #" + (ranks[0] + 1)
                + "\n• " + score2.name() + " — #" + (ranks[1] + 1),
            ButtonType.OK
        ).showAndWait();
      } else {
        Score score = promptAndSaveScore(
            game1.score(),
            game1.highestStreak(),
            game1.elapsed()
        );
        int rank = ctx.getScoresService().add(score) + 1;
        new Alert(Alert.AlertType.INFORMATION, "You placed #" + rank).showAndWait();
      }

      ctx.show(Route.MENU);
      ctx.evict(Route.PLAY);
    });
  }

  /**
   * Prompts for the player name, assembles a {@link Score} using the current session’s start time,
   * and returns it. The {@code name} may be empty if the dialog is dismissed.
   *
   * @param score      final score value
   * @param bestStreak highest streak achieved
   * @param elapsed    elapsed play time
   *
   * @return a {@link Score} ready to be persisted
   */
  private Score promptAndSaveScore(int score, int bestStreak, Duration elapsed) {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Save High Score");
    dialog.setHeaderText(
        "Score: "
            + score
            + " • Streak: "
            + bestStreak
            + " • Time: "
            + TimeUtil.formatDuration(elapsed)
    );
    dialog.setContentText("Enter your name:");
    Optional<String> result = dialog.showAndWait();
    String name = result.map(String::trim).filter(s -> !s.isEmpty()).orElse("");

    return new Score(name, score, bestStreak, now, elapsed);
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
   * present, player 2. Also supports audio toggles and pause.
   *
   * <p><strong>Bindings:</strong></p>
   * <ul>
   *   <li>Pause: {@code P}, {@code ESCAPE}</li>
   *   <li>Player&nbsp;1: {@code LEFT}/{@code RIGHT}/{@code UP} (rotate CW)/{@code DOWN} (soft drop)
   *   </li>
   *   <li>Player&nbsp;2 (extend mode): {@code ,} (left), {@code .} (right), {@code L} (rotate CW),
   *       {@code SPACE} (soft drop). When Player&nbsp;2 is absent, these keys control
   *       Player&nbsp;1.</li>
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
