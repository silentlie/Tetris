package com.example.tetris.helpers;

import static com.example.tetris.controllers.GameView.BACKGROUND;
import static com.example.tetris.controllers.GameView.EMPTY;
import static com.example.tetris.controllers.GameView.GRID;

import com.example.tetris.models.GameState;
import com.example.tetris.models.Tetromino;
import com.example.tetris.models.TetrominoType;

public class BoardCanvas extends ResizableCanvas {

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

    // Board cells
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

    // Grid hairlines (1 device pixel)
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
