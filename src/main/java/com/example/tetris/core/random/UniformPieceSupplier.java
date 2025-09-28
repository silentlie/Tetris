package com.example.tetris.core.random;

import com.example.tetris.core.piece.TetrominoType;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.function.Supplier;

/**
 * Uniform RNG-backed supplier of {@link TetrominoType} values.
 *
 * <p>Each call to {@link #get()} selects one of the seven tetromino types with equal probability
 * (independent draws; this is <i>not</i> a 7-bag generator).</p>
 *
 * <p>Backed by {@link SplittableRandom}, which provides good statistical quality and cheap
 * reproducible streams when seeded deterministically.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * long seed = 1234L;
 * Supplier<TetrominoType> sup = new UniformPieceSupplier(new SplittableRandom(seed));
 * TetrominoType next = sup.get(); // uniformly random piece
 * }</pre>
 *
 * <p>This class is thread-safe <i>only if</i> the provided {@code SplittableRandom} is not shared
 * across threads, or if callers provide their own synchronization.</p>
 *
 * @since 1.0
 */
public final class UniformPieceSupplier implements Supplier<TetrominoType> {

  private final SplittableRandom rng;
  private final TetrominoType[] pool = TetrominoType.values();

  /**
   * Creates a supplier that draws uniformly from {@link TetrominoType#values()} using the given
   * RNG.
   *
   * @param rng the random source (must not be {@code null})
   */
  public UniformPieceSupplier(SplittableRandom rng) {
    this.rng = Objects.requireNonNull(rng, "rng");
  }

  /**
   * Returns a uniformly random tetromino type (independent draw).
   *
   * @return a random {@link TetrominoType} (never {@code null})
   */
  @Override
  public TetrominoType get() {
    return pool[rng.nextInt(pool.length)];
  }
}
