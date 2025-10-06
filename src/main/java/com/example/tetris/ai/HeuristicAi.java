package com.example.tetris.ai;

import com.example.tetris.core.board.Cell;
import com.example.tetris.core.piece.Rotation;
import com.example.tetris.core.piece.Tetromino;
import com.example.tetris.core.piece.TetrominoType;
import com.example.tetris.core.state.GameIntent;
import com.example.tetris.core.state.OpMove;
import com.example.tetris.core.state.PureGame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HeuristicAi extends Ai {

  private static final double WEIGHT_LINES = +1.00;
  private static final double WEIGHT_TOTAL_HEIGHT = -0.50;
  private static final double WEIGHT_HOLES = -1.80;
  private static final double WEIGHT_BUMPINESS = -0.25;
  private static final double WEIGHT_MAX_HEIGHT = -0.10;
  private static final double WEIGHT_SPIRE = -0.30;
  private static final double WEIGHT_WELL = -0.50;

  private static final double ALPHA = 0.5;

  private final double weightLines;
  private final double weightTotalHeight;
  private final double weightHoles;
  private final double weightBumpiness;
  private final double weightMaxHeight;
  private final double weightSpire;
  private final double weightWell;

  public HeuristicAi() {
    this(WEIGHT_LINES, WEIGHT_TOTAL_HEIGHT, WEIGHT_HOLES, WEIGHT_BUMPINESS, WEIGHT_MAX_HEIGHT,
        WEIGHT_SPIRE, WEIGHT_WELL);
  }

  public HeuristicAi(double weightLines, double weightTotalHeight, double weightHoles,
      double weightBumpiness, double weightMaxHeight, double weightSpire, double weightWell) {
    this.weightLines = weightLines;
    this.weightTotalHeight = weightTotalHeight;
    this.weightHoles = weightHoles;
    this.weightBumpiness = weightBumpiness;
    this.weightMaxHeight = weightMaxHeight;
    this.weightSpire = weightSpire;
    this.weightWell = weightWell;
  }

  private record Eval(double score, int[][] afterClear) {

  }

  private static List<Integer> possibleX(int[][] grid, Tetromino active) {
    List<Integer> xs = new ArrayList<>();
    Tetromino cur = active;
    while (BoardSimulator.fits(grid, cur)) {
      xs.add(cur.position().x());
      cur = cur.moveLeft();
    }
    cur = active.moveRight();
    while (BoardSimulator.fits(grid, cur)) {
      xs.add(cur.position().x());
      cur = cur.moveRight();
    }
    Collections.sort(xs);
    return xs;
  }

  private static List<Rotation> uniqueRotations(TetrominoType type) {
    return switch (type) {
      case O -> List.of(Rotation.UP);
      case I, S, Z -> List.of(Rotation.UP, Rotation.RIGHT);
      default -> Arrays.stream(Rotation.values()).toList();
    };
  }

  private static int cwSteps(Rotation from, Rotation to) {
    return (to.ordinal() - from.ordinal()) & 3;
  }

  @Override
  protected CompletableFuture<OpMove> submit(PureGame snapshot) {
    if (snapshot == null || snapshot.tetromino() == null) {
      return CompletableFuture.completedFuture(null);
    }
    Tetromino active = snapshot.tetromino();
    int[][] grid = snapshot.cells();
    double bestTotal = Double.NEGATIVE_INFINITY;
    OpMove bestMove = null;

    for (Rotation rot : uniqueRotations(active.type())) {
      Tetromino rotated = active.withRotation(rot);
      for (int x : possibleX(grid, rotated)) {
        Tetromino moved = rotated.withPosition(new Cell(x, active.position().y()));
        Eval parent = evalPlacement(grid, moved);
        if (parent == null) {
          continue;
        }

        double child = 0.0;
        if (snapshot.next() != null) {
          child = computeNext(parent.afterClear(), snapshot.next());
        }

        double total = parent.score() + ALPHA * child;
        if (total > bestTotal) {
          bestTotal = total;
          bestMove = new OpMove(x, cwSteps(active.rotation(), rot));
        }
      }
    }
    return CompletableFuture.completedFuture(bestMove);
  }

  @Override
  public void shutdown() {

  }

  @Override
  protected GameIntent whenAligned(OpMove opMove, PureGame snapshot) {
    return GameIntent.SOFT_DROP;
  }

  private Eval evalPlacement(int[][] grid, Tetromino placed) {
    if (!BoardSimulator.fits(grid, placed)) {
      return null;
    }
    int[][] after = BoardSimulator.hardDrop(grid, placed);
    int cleared = BoardEvaluator.countFullRows(after);
    after = BoardSimulator.clearLines(after);
    double s = score(after, cleared);
    return new Eval(s, after);
  }

  private double computeNext(int[][] grid, TetrominoType type) {
    double best = Double.NEGATIVE_INFINITY;
    for (Rotation rotation : uniqueRotations(type)) {
      for (int x = 0; x < BoardEvaluator.getWidth(grid); x++) {
        Tetromino maybe = new Tetromino(type, new Cell(x, -2), rotation);
        Eval ev = evalPlacement(grid, maybe);
        if (ev != null && ev.score() > best) {
          best = ev.score();
        }
      }
    }
    return (best == Double.NEGATIVE_INFINITY) ? -1e9 : best;
  }

  private double score(int[][] grid, int cleared) {
    BoardEvaluator stats = BoardEvaluator.of(grid);
    return weightLines * cleared + weightTotalHeight * stats.totalHeight + weightHoles * stats.holes
        + weightBumpiness * stats.bumpiness + weightMaxHeight * stats.maxHeight
        + weightSpire * stats.spireSpread + weightWell * stats.wellSpread;
  }
}
