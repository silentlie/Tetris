package com.example.tetris.ui.canvas;

import static com.example.tetris.ui.view.GameView.BACKGROUND;
import static com.example.tetris.ui.view.GameView.GRID;

import com.example.tetris.core.board.Cell;
import com.example.tetris.core.piece.Rotation;
import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.piece.TetrominoType;
import java.util.List;

public class NextCanvas extends ResizableCanvas {

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
