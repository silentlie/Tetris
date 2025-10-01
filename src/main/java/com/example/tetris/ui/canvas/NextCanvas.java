package com.example.tetris.ui.canvas;

import static com.example.tetris.ui.view.GameView.BACKGROUND;
import static com.example.tetris.ui.view.GameView.GRID;

import com.example.tetris.core.board.Cell;
import com.example.tetris.core.piece.Rotation;
import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.piece.TetrominoType;
import java.util.List;

/**
 * Canvas that renders the “next pieces” preview list.
 *
 * <p>Displays up to five upcoming {@link TetrominoType} entries in vertically stacked boxes.
 * Each preview is centered inside a 4×4 logical cell box and drawn in the piece’s display color,
 * with a thin grid stroke for clarity.</p>
 *
 * <p>Rendering occurs only when the canvas is marked dirty; after a successful render the dirty
 * flag is cleared. Size changes or external updates should call {@link #markDirty()}.</p>
 *
 * <p>Threading: interact with this node on the JavaFX application thread.</p>
 *
 * @since 1.0
 */
public class NextCanvas extends ResizableCanvas {

  /**
   * Renders the preview list if this canvas is dirty.
   *
   * <p>Behavior:</p>
   * <ul>
   *   <li>Fills the background.</li>
   *   <li>If {@code queue} is null or empty, clears the dirty flag and returns.</li>
   *   <li>Otherwise, shows up to five upcoming pieces, each in its own vertical slot.</li>
   * </ul>
   *
   * @param queue upcoming pieces (head-first); may be {@code null} or empty
   */
  public void render(List<TetrominoType> queue) {
    if (!dirty) {
      return;
    }

    double w = canvas.getWidth();
    double h = canvas.getHeight();

    gc.setFill(BACKGROUND);
    gc.fillRect(0, 0, w, h);

    if (queue == null || queue.isEmpty()) {
      dirty = false;
      return;
    }

    int show = Math.min(queue.size(), 5);
    double perBoxH = h / show;

    for (int i = 0; i < show; i++) {
      drawPreview(queue.get(i), 0, i * perBoxH, w, perBoxH);
    }

    dirty = false;
  }

  /**
   * Draws a single piece preview centered within a rectangle.
   *
   * <p>The preview is laid out into a 4×4 logical cell area. The method computes an integer
   * cell size that fits inside the padded rectangle, centers the rotated shape within 4×4 by using
   * its bounding box, and then fills/strokes each occupied cell.</p>
   *
   * @param type the tetromino type to draw
   * @param x    the left of the target rectangle (canvas coordinates)
   * @param y    the top of the target rectangle (canvas coordinates)
   * @param w    the width of the target rectangle
   * @param h    the height of the target rectangle
   */
  private void drawPreview(TetrominoType type, double x, double y, double w, double h) {
    double pad = Math.floor(Math.min(w, h) * 0.08);
    double boxW = Math.max(0, w - 2 * pad);
    double boxH = Math.max(0, h - 2 * pad);
    double cell = Math.floor(Math.min(boxW, boxH) / 4.0);
    if (cell < 1) {
      cell = 1;
    }

    double ox = x + (w - cell * 4) / 2.0;
    double oy = y + (h - cell * 4) / 2.0;

    gc.save();
    gc.translate(ox, oy);
    gc.scale(cell, cell);

    Tetromino preview = new Tetromino(type, new Cell(0, 0), Rotation.UP);
    var shape = preview.getAbsoluteShape();

    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;
    for (var c : shape) {
      minX = Math.min(minX, c.x());
      minY = Math.min(minY, c.y());
      maxX = Math.max(maxX, c.x());
      maxY = Math.max(maxY, c.y());
    }
    int pw = (maxX - minX + 1);
    int ph = (maxY - minY + 1);
    double cx = (4 - pw) / 2.0 - minX;
    double cy = (4 - ph) / 2.0 - minY;

    // draw tetromino
    gc.setFill(type.getColor());
    for (var c : shape) {
      gc.fillRect(c.x() + cx, c.y() + cy, 1, 1);
    }

    gc.setLineWidth(1.0 / Math.max(1.0, cell));
    gc.setStroke(GRID);
    for (var c : shape) {
      gc.strokeRect(c.x() + cx, c.y() + cy, 1, 1);
    }

    gc.restore();
  }
}
