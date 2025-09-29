package com.example.tetris.core.random;

import com.example.tetris.core.piece.TetrominoType;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Strategies for supplying {@link TetrominoType} sequences.
 *
 * <p>Each enum constant wraps a factory that produces a
 * {@link Supplier}&lt;{@link TetrominoType}&gt; given a {@link SplittableRandom}. Use
 * {@link #create(SplittableRandom)} when you already have an RNG stream, or {@link #create(long)}
 * to build one from a seed.</p>
 *
 * <p>Available strategies:</p>
 * <ul>
 *   <li>{@link #UNIFORM} — independent uniform draws over the seven types (no 7-bag smoothing).
 *   </li>
 *   <li>{@link #SEVEN_BAG} — modern 7-bag generator: each bag contains one of each type in a
 *       shuffled order, drastically reducing droughts.</li>
 * </ul>
 *
 * <p>Both strategies are deterministic when the provided RNG is seeded deterministically.</p>
 */
public enum PieceSupplierStrategy {

  /**
   * Uniform, independent draws over {@link TetrominoType#values()}.
   *
   * <p>Backed by {@link UniformPieceSupplier}. Suitable for classic/arcade variants or testing
   * pure randomness.</p>
   */
  UNIFORM(UniformPieceSupplier::new),

  /**
   * 7-bag generator: emits each type exactly once per bag in random order.
   *
   * <p>Backed by {@link SevenBagPieceSupplier}. This is the most common modern distribution and
   * mitigates long droughts (e.g., no endless wait for an {@code I}).</p>
   */
  SEVEN_BAG(SevenBagPieceSupplier::new);

  private final Function<SplittableRandom, Supplier<TetrominoType>> factory;

  PieceSupplierStrategy(Function<SplittableRandom, Supplier<TetrominoType>> factory) {
    this.factory = Objects.requireNonNull(factory);
  }

  /**
   * Creates a piece supplier using the given RNG.
   *
   * <p>The supplier’s determinism and thread-safety match the provided {@link SplittableRandom}
   * and the chosen implementation.</p>
   *
   * @param rng the random source (must not be {@code null})
   *
   * @return a supplier of {@link TetrominoType} according to this strategy
   */
  public Supplier<TetrominoType> create(SplittableRandom rng) {
    return factory.apply(rng);
  }

  /**
   * Creates a piece supplier using a new {@link SplittableRandom} seeded with {@code seed}.
   *
   * <p>Convenience overload for deterministic streams without manually constructing the RNG.</p>
   *
   * @param seed seed for the RNG
   *
   * @return a supplier of {@link TetrominoType} according to this strategy
   */
  public Supplier<TetrominoType> create(long seed) {
    return create(new SplittableRandom(seed));
  }
}
