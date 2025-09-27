package com.example.tetris.core.state;

import com.example.tetris.core.board.Board;
import com.example.tetris.core.board.Cell;
import com.example.tetris.core.piece.Rotation;
import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.piece.TetrominoType;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

public class GameState {

  private final Board board;
  private final PieceQueue queue;

  private Tetromino tetromino;
  private int score = 0;
  private int streak = 0;
  private int highestStreak = 0;
  private boolean gameOver = false;

  public GameState(Board board, PieceQueue queue) {
    this.board = Objects.requireNonNull(board, "board is null");
    this.queue = Objects.requireNonNull(queue, "queue is null");
    tetromino = spawnFromQueue();
    if (!board.fits(tetromino)) {
      gameOver = true;
    }
  }

  public boolean apply(GameIntent intent) {
    if (gameOver) {
      return false;
    }
    return switch (intent) {
      case ROTATE_CW -> tryApply(Tetromino::rotateClockwise);
      case ROTATE_CCW -> tryApply(Tetromino::rotateCounterClockwise);
      case MOVE_LEFT -> tryApply(Tetromino::moveLeft);
      case MOVE_RIGHT -> tryApply(Tetromino::moveRight);
      case SOFT_DROP -> {
        if (!tryApply(Tetromino::moveDown)) {
          lock();
        }
        yield true;
      }
      case HARD_DROP -> {
        while (true) {
          if (!tryApply(Tetromino::moveDown)) {
            break;
          }
        }
        lock();
        yield true;
      }
    };
  }

  private boolean tryApply(UnaryOperator<Tetromino> op) {
    Tetromino candidate = op.apply(tetromino);
    if (board.fits(candidate)) {
      tetromino = candidate;
      return true;
    }
    return false;
  }

  private void lock() {
    boolean lockOut = board.lock(tetromino);
    if (lockOut) {
      gameOver = true;
      return;
    }
    int cleared = board.clearLines();
    if (cleared == 0) {
      streak = 0;
    } else {
      streak += cleared;
      highestStreak = Math.max(highestStreak, streak);
      score += calculateScoring(cleared, streak);
    }
    tetromino = spawnFromQueue();
    if (!board.fits(tetromino)) {
      gameOver = true;
    }
  }

  private int calculateScoring(int cleared, int streak) {
    int scoring = 0;
    while (cleared-- > 0) {
      scoring += streak * 100;
    }
    return scoring;
  }

  private Tetromino spawnFromQueue() {
    TetrominoType type = queue.popAndRefill();
    Cell position = new Cell(board.width / 2 - 1, -2);
    return new Tetromino(type, position, Rotation.UP);
  }

  // ===== Read-only access for rendering/HUD =====

  public int getScore() {
    return score;
  }

  public int getStreak() {
    return streak;
  }

  public int getHighestStreak() {
    return highestStreak;
  }

  public boolean isGameOver() {
    return gameOver;
  }

  public int getWidth() {
    return board.width;
  }

  public int getHeight() {
    return board.height;
  }

  public TetrominoType getCell(int x, int y) {
    return board.getCell(x, y);
  }

  public Tetromino getTetromino() {
    return tetromino;
  }

  public List<TetrominoType> getQueue() {
    return queue.peek();
  }

  public PureGame toCells() {
    int[][] cells = new int[getHeight()][getWidth()];
    for (int y = 0; y < getHeight(); y++) {
      for (int x = 0; x < getWidth(); x++) {
        cells[y][x] = (getCell(x, y) == null) ? 0 : 1;
      }
    }
    int[][] currentShape = toShapeMatrix(tetromino.getAbsoluteShape());
    List<TetrominoType> peek = getQueue();
    TetrominoType nextType = peek.getFirst();
    Tetromino preview = new Tetromino(nextType, new Cell(0, 0), Rotation.UP);
    int[][] nextShape = toShapeMatrix(preview.getAbsoluteShape());
    return new PureGame(getWidth(), getHeight(), cells, currentShape, nextShape);
  }

  private static int[][] toShapeMatrix(List<Cell> cells) {
    if (cells == null || cells.isEmpty()) {
      return new int[0][0];
    }

    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;

    for (Cell c : cells) {
      if (c.x() < minX) {
        minX = c.x();
      }
      if (c.y() < minY) {
        minY = c.y();
      }
      if (c.x() > maxX) {
        maxX = c.x();
      }
      if (c.y() > maxY) {
        maxY = c.y();
      }
    }

    int w = maxX - minX + 1;
    int h = maxY - minY + 1;
    int[][] bits = new int[h][w];

    for (Cell c : cells) {
      int nx = c.x() - minX;
      int ny = c.y() - minY;
      if (ny >= 0 && ny < h && nx >= 0 && nx < w) {
        bits[ny][nx] = 1;
      }
    }
    return bits;
  }
}
