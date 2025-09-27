package com.example.tetris.core.board;

import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.piece.TetrominoType;
import java.util.Arrays;

public class Board {

  public final int width;
  public final int height;
  private final TetrominoType[][] board;
  private final int[] filled;

  public Board(int width, int height) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Invalid board size");
    }
    this.width = width;
    this.height = height;
    this.board = new TetrominoType[height][width];
    this.filled = new int[height];
  }

  public boolean fits(Tetromino t) {
    for (Cell cell : t.getAbsoluteShape()) {
      int x = cell.x();
      int y = cell.y();
      // OOB
      if (x < 0 || x >= width) {
        return false;
      }
      // allow above top
      if (y < 0) {
        continue;
      }
      // below floor
      if (y >= height) {
        return false;
      }
      // collision
      if (board[y][x] != null) {
        return false;
      }
    }
    return true;
  }

  public boolean lock(Tetromino t) {
    boolean lockOut = false;
    for (Cell c : t.getAbsoluteShape()) {
      int x = c.x();
      int y = c.y();
      if (y < 0) {
        lockOut = true;
        continue;
      }
      if (x < 0 || x >= width || y >= height || board[y][x] != null) {
        // Illegal call: tried to lock without fits().
        throw new IllegalStateException("Lock with OOB cell: (" + x + "," + y + ")");
      }
      board[y][x] = t.type();
      filled[y]++;
    }
    return lockOut;
  }

  public int clearLines() {
    int cleared = 0;
    int writeY = height - 1;
    for (int y = height - 1; y >= 0; y--) {
      if (filled[y] == width) {
        cleared++;
      } else {
        if (y != writeY) {
          TetrominoType[] tmp = board[writeY];
          board[writeY] = board[y];
          board[y] = tmp;
          int f = filled[writeY];
          filled[writeY] = filled[y];
          filled[y] = f;
        }
        writeY--;
      }
    }
    for (int y = 0; y < cleared; y++) {
      Arrays.fill(board[y], null);
      filled[y] = 0;
    }
    return cleared;
  }


  public TetrominoType getCell(Cell c) {
    int x = c.x();
    int y = c.y();
    if (x < 0 || x >= width || y < 0 || y >= height) {
      return null;
    }
    return board[y][x];
  }

  public TetrominoType getCell(int x, int y) {
    if (x < 0 || x >= width || y < 0 || y >= height) {
      return null;
    }
    return board[y][x];
  }
}
