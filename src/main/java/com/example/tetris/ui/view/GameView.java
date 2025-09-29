package com.example.tetris.ui.view;

import static com.example.tetris.util.TimeUtil.formatElapsedNanos;

import com.example.tetris.Main;
import com.example.tetris.core.rule.GameClock;
import com.example.tetris.core.state.GameState;
import com.example.tetris.ui.canvas.BoardCanvas;
import com.example.tetris.ui.canvas.NextCanvas;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.paint.Color;

/**
 * JavaFX view that displays the running game: HUD labels, board canvas, and next-piece canvas.
 *
 * <p>Loads its layout from {@code /views/game-view.fxml} (resolved relative to {@link Main}).
 * The FXML defines labels for HUD metrics and two custom canvases: {@link BoardCanvas} and
 * {@link NextCanvas}.</p>
 *
 * <p>The view maintains a local {@code dirty} flag and propagates dirtiness to its canvases.
 * Rendering is pull-driven via {@link #render(GameState, GameClock)}; the method updates HUD labels
 * every call and repaints canvases only when dirty.</p>
 *
 * <p>Threading: interact with this view on the JavaFX application thread.</p>
 *
 * @since 1.0
 */
public class GameView extends SplitPane {

  /**
   * Grid stroke color (hairlines).
   */
  public static final Color GRID = Color.web("#2a2a2a");

  /**
   * Background color behind canvases.
   */
  public static final Color BACKGROUND = Color.web("#111111");

  /**
   * Fill color for empty board board.
   */
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
  private Label labelHighestStreak;
  @FXML
  private Label labelTimeElapsed;
  @FXML
  private BoardCanvas boardCanvas;
  @FXML
  private NextCanvas nextCanvas;

  /**
   * Local dirty bit; when true, canvases will be repainted on {@link #render}.
   */
  private boolean dirty = false;

  /**
   * Constructs the view and loads {@code game-view.fxml}.
   *
   * <p>Sets this instance as both root and controller of the FXML.</p>
   *
   * @throws RuntimeException if the FXML fails to load
   */
  public GameView() {
    FXMLLoader fxml = new FXMLLoader(Main.class.getResource("views/game-view.fxml"));
    fxml.setRoot(this);
    fxml.setController(this);
    try {
      fxml.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load GameView.fxml", e);
    }
  }

  /**
   * FXML-initialization hook.
   *
   * <p>Marks the view dirty whenever its size changes. Canvases are resized by layout and will
   * be repainted on the next {@link #render} call.</p>
   */
  @FXML
  private void initialize() {
    widthProperty().addListener((o, oldV, newV) -> markDirty());
    heightProperty().addListener((o, oldV, newV) -> markDirty());
  }

  /**
   * Marks the view as needing layout-driven repaint after the superclass lays out children.
   */
  @Override
  protected void layoutChildren() {
    super.layoutChildren();
    markDirty();
  }

  /**
   * Marks this view and its child canvases as dirty.
   *
   * <p>Call this after model changes or resizes to repaint on the next {@link #render} call.</p>
   */
  public void markDirty() {
    dirty = true;
    if (boardCanvas != null) {
      boardCanvas.markDirty();
    }
    if (nextCanvas != null) {
      nextCanvas.markDirty();
    }
  }

  /**
   * Updates HUD labels and repaints canvases if this view is dirty.
   *
   * <p>Labels are refreshed every call from {@code clock} and {@code s}. Canvases repaint only
   * when {@link #dirty} is {@code true}; after repainting, the flag is cleared.</p>
   *
   * @param s     the current game state (must not be {@code null})
   * @param clock the game clock providing level and elapsed time (must not be {@code null})
   */
  public void render(GameState s, GameClock clock) {
    labelInitialLevel.setText("Initial Level: " + clock.startLevel);
    labelCurrentLevel.setText("Current Level: " + clock.level());
    labelScore.setText("Score: " + s.getScore());
    labelStreak.setText("Streak: " + s.getStreak());
    labelHighestStreak.setText("Highest Streak: " + s.getHighestStreak());
    labelTimeElapsed.setText("Time Elapsed: " + formatElapsedNanos(clock.elapsedNanos()));

    if (!dirty) {
      return;
    }
    if (boardCanvas != null) {
      boardCanvas.render(s);
    }
    if (nextCanvas != null) {
      nextCanvas.render(s.getQueue());
    }
    dirty = false;
  }
}
