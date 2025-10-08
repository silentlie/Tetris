package com.example.tetris.ui.canvas;

import static com.example.tetris.ui.view.GameView.BACKGROUND;
import static com.example.tetris.ui.view.GameView.EMPTY;
import static com.example.tetris.ui.view.GameView.GRID;

import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.piece.TetrominoType;
import com.example.tetris.core.state.GameState;
import com.example.tetris.ui.view.GameView;

/**
 * Canvas that renders the Tetris board, settled blocks, and the active tetromino.
 *
 * <h2>Layout &amp; scaling</h2>
 * <ul>
 *   <li>The internal {@link javafx.scene.canvas.Canvas} is bound to this region’s size
 *       via {@link ResizableCanvas}.</li>
 *   <li>The board is letterboxed to preserve aspect ratio and centered within the available area.
 *   </li>
 *   <li>Rendering uses a cell-space transform where 1 canvas unit equals 1 board cell.</li>
 * </ul>
 *
 * <h2>Visual layers (back → front)</h2>
 * <ol>
 *   <li>Background fill ({@link GameView#BACKGROUND}).</li>
 *   <li>Settled cells using each {@link TetrominoType#getColor()} or {@link GameView#EMPTY} for
 *   empty.</li>
 *   <li>Active tetromino cells with its type color.</li>
 *   <li>Grid hairlines in {@link GameView#GRID} at ~1 device pixel thickness (scale-adjusted).</li>
 * </ol>
 *
 * <h2>Dirty flag</h2>
 * The {@code dirty} flag (inherited from {@link ResizableCanvas}) is cleared after a successful
 * render. Call {@link #markDirty()} when the game state changes or on resize to schedule a redraw.
 *
 * <h2>Threading</h2>
 * Invoke {@link #render(GameState)} on the JavaFX application thread.
 *
 * <h2>Notes</h2>
 * <ul>
 *   <li>Negative coordinates (e.g., a spawning piece above the top) are culled by the canvas
 *   viewport—no special handling is required.</li>
 *   <li>Grid line width is computed as {@code 1 / max(1, cellSize)} so it approximates one device
 *   pixel regardless of zoom.</li>
 *   <li>This class performs no allocations per-frame other than trivial loops; colors are reused.
 *   </li>
 * </ul>
 *
 * @since 1.0
 */
public class BoardCanvas extends ResizableCanvas {

  /**
   * Renders the provided {@link GameState} if this canvas is marked dirty.
   *
   * <p>Early-exits if {@code s == null} or {@code dirty == false}.</p>
   *
   * <p>Steps:</p>
   * <ol>
   *   <li>Compute the maximum integer cell size that fits the board within the canvas.</li>
   *   <li>Center the board, set a transform so 1 unit equals 1 cell, and draw the background.</li>
   *   <li>Paint settled cells from {@link GameState#getCell(int, int)}.</li>
   *   <li>Paint the active {@link Tetromino} from {@link Tetromino#getAbsoluteShape()}.</li>
   *   <li>Overlay grid lines with scale-adjusted line width (≈ 1 device pixel).</li>
   * </ol>
   *
   * @param s the current game state to render; ignored if {@code null}
   */
  public void render(GameState s) {
    if (!dirty || s == null) {
      return;
    }

    double width = canvas.getWidth();
    double height = canvas.getHeight();

    // Background
    gc.setFill(BACKGROUND);
    gc.fillRect(0, 0, width, height);

    int cols = s.getWidth();
    int rows = s.getHeight();

    double cell = Math.floor(Math.min(width / cols, height / rows));
    double boardW = cell * cols;
    double boardH = cell * rows;
    double ox = (width - boardW) / 2.0;
    double oy = (height - boardH) / 2.0;

    gc.save();
    gc.translate(ox, oy);
    gc.scale(cell, cell); // 1 unit == 1 cell

    // Settled cells
    for (int y = 0; y < rows; y++) {
      for (int x = 0; x < cols; x++) {
        TetrominoType type = s.getCell(x, y);
        gc.setFill(type != null ? type.getColor() : EMPTY);
        gc.fillRect(x, y, 1, 1);
      }
    }

    // Active tetromino
    Tetromino t = s.getTetromino();
    if (t != null) {
      gc.setFill(t.type().getColor());
      for (var c : t.getAbsoluteShape()) {
        gc.fillRect(c.x(), c.y(), 1, 1);
      }
    }

    // Grid hairlines (~1 device pixel)
    gc.setLineWidth(1.0 / Math.max(1.0, cell));
    gc.setStroke(GRID);
    for (int x = 0; x <= cols; x++) {
      gc.strokeLine(x, 0, x, rows);
    }
    for (int y = 0; y <= rows; y++) {
      gc.strokeLine(0, y, cols, y);
    }

    gc.restore();

    dirty = false;
  }
}
