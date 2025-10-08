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

/**
 * Single-step, linear-evaluation Tetris AI with a shallow lookahead.
 *
 * <p>The agent enumerates unique rotations and reachable X positions for the
 * active piece, simulates a hard drop + line clear, then scores the resulting board using a linear
 * combination of hand-tuned features from {@link BoardEvaluator}. A discounted one-piece lookahead
 * (next queue head) is optionally folded in via {@link #ALPHA}.</p>
 *
 * <h2>Search sketch</h2>
 * <ol>
 *   <li>For each unique rotation of the active {@link TetrominoType}, sweep
 *       left/right to collect all X positions where the piece
 *       {@link BoardSimulator#fits(int[][], Tetromino)}.</li>
 *   <li>For each candidate: hard-drop, clear lines, and {@link #score(int[][], int)}.</li>
 *   <li>Optionally evaluate the best immediate placement for {@code next} and
 *       blend it with weight {@link #ALPHA}.</li>
 *   <li>Return an {@link OpMove} with the best-scoring X and clockwise-rotation
 *       steps from the current rotation.</li>
 * </ol>
 *
 * <p><b>Immutability/Threading:</b> the AI is stateless after construction and
 * safe to share across games/threads as long as callers do not modify the input
 * arrays they pass in. The returned {@code CompletableFuture} is already
 * completed; asynchronous composition is supported by the caller if desired.</p>
 */
public class HeuristicAi extends Ai {

  /**
   * Reward per completed row (applied to the <em>parent</em> state).
   */
  private static final double WEIGHT_LINES = 1.00;
  /**
   * Penalty on the sum of column heights.
   */
  private static final double WEIGHT_TOTAL_HEIGHT = -0.50;
  /**
   * Penalty on holes (empty cells beneath the first filled cell in a column).
   */
  private static final double WEIGHT_HOLES = -1.80;
  /**
   * Penalty on adjacent height deltas.
   */
  private static final double WEIGHT_BUMPINESS = -0.25;
  /**
   * Penalty on the tallest column.
   */
  private static final double WEIGHT_MAX_HEIGHT = -0.10;
  /**
   * Penalty on spire spread (tallest above median).
   */
  private static final double WEIGHT_SPIRE = -0.30;
  /**
   * Penalty on well spread (median above lowest).
   */
  private static final double WEIGHT_WELL = -0.50;

  /**
   * Lookahead blending factor in {@code total = parent + ALPHA * bestChild}. Set to {@code 0} for
   * pure greedy; {@code 1} to value the immediate child equally with the parent.
   */
  private static final double ALPHA = 0.5;

  private final double weightLines;
  private final double weightTotalHeight;
  private final double weightHoles;
  private final double weightBumpiness;
  private final double weightMaxHeight;
  private final double weightSpire;
  private final double weightWell;

  /**
   * Uses the default weight vector above.
   */
  public HeuristicAi() {
    this(WEIGHT_LINES, WEIGHT_TOTAL_HEIGHT, WEIGHT_HOLES, WEIGHT_BUMPINESS, WEIGHT_MAX_HEIGHT,
        WEIGHT_SPIRE, WEIGHT_WELL);
  }

  /**
   * Allows callers to inject a custom linear weight vector.
   */
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

  /**
   * Pair of (evaluation score, post-clear grid) for a simulated placement.
   */
  private record Eval(double score, int[][] afterClear) {

  }

  /**
   * Returns the inclusive set of X coordinates reachable by horizontal slides from the given
   * {@code active} placement, without rotations during motion. The starting X is included iff
   * {@link BoardSimulator#fits(int[][], Tetromino)}.
   */
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

  /**
   * Minimal unique rotation set per piece type to avoid duplicates under symmetry. O has a single
   * unique orientation; I/S/Z have 2; others have 4.
   */
  private static List<Rotation> uniqueRotations(TetrominoType type) {
    return switch (type) {
      case O -> List.of(Rotation.UP);
      case I, S, Z -> List.of(Rotation.UP, Rotation.RIGHT);
      default -> Arrays.stream(Rotation.values()).toList();
    };
  }

  /**
   * Returns clockwise quarter-turns to go from {@code from} to {@code to}.
   */
  private static int cwSteps(Rotation from, Rotation to) {
    return (to.ordinal() - from.ordinal()) & 3;
  }

  /**
   * Produces the best {@link OpMove} for the current snapshot by greedy search with discounted
   * one-piece lookahead.
   *
   * <p>The returned future is already completed; this method is synchronous.</p>
   */
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

  /**
   * No resources to release for this implementation.
   */
  @Override
  public void shutdown() {
  }

  @Override
  protected GameIntent whenAligned(OpMove opMove, PureGame snapshot) {
    return GameIntent.SOFT_DROP;
  }

  /**
   * Simulates dropping {@code placed}, clears any full rows, and evaluates the resulting board.
   * Returns {@code null} if the placement collides.
   */
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

  /**
   * Best immediate evaluation obtainable by dropping a single piece of {@code type} on
   * {@code grid}. Returns a very negative sentinel if no legal placement exists.
   */
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

  /**
   * Linear scoring function over {@link BoardEvaluator} features for the
   * <em>post-clear</em> board, plus a reward for lines cleared by the parent drop.
   */
  private double score(int[][] grid, int cleared) {
    BoardEvaluator stats = BoardEvaluator.of(grid);
    return weightLines * cleared
        + weightTotalHeight * stats.totalHeight
        + weightHoles * stats.holes
        + weightBumpiness * stats.bumpiness
        + weightMaxHeight * stats.maxHeight
        + weightSpire * stats.spireSpread
        + weightWell * stats.wellSpread;
  }
}
