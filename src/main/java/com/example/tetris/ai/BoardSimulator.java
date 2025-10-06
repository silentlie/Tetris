package com.example.tetris.ai;

import com.example.tetris.core.board.Cell;
import com.example.tetris.core.piece.Tetromino;
import java.util.Arrays;

public class BoardSimulator {

  public static int[][] deepCopy(int[][] original) {
    int[][] copy = new int[original.length][];
    for (int i = 0; i < original.length; i++) {
      copy[i] = Arrays.copyOf(original[i], original[i].length);
    }
    return copy;
  }

  public static int[][] clearLines(int[][] original) {
    int[][] copy = new int[original.length][];
    int write = original.length - 1;

    for (int y = original.length - 1; y >= 0; y--) {
      int[] row = original[y];
      boolean full = true;
      for (int cell : row) {
        if (cell == 0) {
          full = false;
          break;
        }
      }
      if (!full) {
        copy[write--] = Arrays.copyOf(row, row.length);
      }
    }

    // Fill remaining rows with empty rows (all zeros)
    int cols = original.length > 0 ? original[0].length : 0;
    while (write >= 0) {
      copy[write--] = new int[cols];
    }
    return copy;
  }

  public static int[][] hardDrop(int[][] original, Tetromino tetromino) {
    if (tetromino == null || !fits(original, tetromino)) {
      return deepCopy(original);
    }
    for (Tetromino next = tetromino.moveDown(); fits(original, next); next = next.moveDown()) {
      tetromino = next;
    }
    return placePiece(original, tetromino);
  }

  public static boolean fits(int[][] original, Tetromino tetromino) {
    int height = BoardEvaluator.getHeight(original);
    int width = BoardEvaluator.getWidth(original);
    for (Cell cell : tetromino.getAbsoluteShape()) {
      int x = cell.x();
      int y = cell.y();
      if (x < 0 || x >= width || y >= height || y >= 0 && original[y][x] != 0) {
        return false;
      }
    }
    return true;
  }

  private static int[][] placePiece(int[][] original, Tetromino tetromino) {
    int[][] copy = deepCopy(original);
    for (Cell cell : tetromino.getAbsoluteShape()) {
      if (cell.y() >= 0) {
        copy[cell.y()][cell.x()] = 1;
      }
    }
    return copy;
  }
}
