package com.example.tetris.core.random;

import com.example.tetris.core.piece.TetrominoType;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.function.Supplier;

/**
 * RNG-backed 7-bag supplier of {@link TetrominoType} values.
 *
 * <p>Emits each of the seven tetromino types exactly once per “bag” in a uniformly random order.
 * When a bag is exhausted, a new bag is produced by shuffling the seven types and appended to the
 * internal queue. This yields the familiar modern Tetris distribution with reduced droughts
 * compared to independent uniform draws.</p>
 *
 * <p>Shuffling uses Fisher–Yates over the {@link TetrominoType#values()} array with the provided
 * {@link SplittableRandom}. Seeding the RNG deterministically makes the emitted sequence
 * reproducible.</p>
 *
 * <p>This class is not inherently thread-safe. Use one instance per thread or guard accesses
 * externally.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * long seed = 42L;
 * SevenBagPieceSupplier sup = new SevenBagPieceSupplier(new SplittableRandom(seed));
 * for (int i = 0; i < 14; i++) {
 *   System.out.println(sup.get()); // 7 unique types, then another 7, each bag shuffled
 * }
 * }</pre>
 *
 * @since 1.0
 */
public final class SevenBagPieceSupplier implements Supplier<TetrominoType> {

  private final SplittableRandom rng;
  private final TetrominoType[] pool = TetrominoType.values();
  private final Deque<TetrominoType> bag = new ArrayDeque<>(TetrominoType.values().length);

  /**
   * Creates a 7-bag supplier driven by the given RNG.
   *
   * @param rng the random source (must not be {@code null})
   */
  public SevenBagPieceSupplier(SplittableRandom rng) {
    this.rng = Objects.requireNonNull(rng, "rng");
    refill();
  }

  /**
   * Replenishes the internal bag by shuffling the seven types and enqueueing them.
   *
   * <p>Implements in-place Fisher–Yates over {@link #pool} using {@link #rng} and
   * appends the result to {@link #bag}.</p>
   */
  private void refill() {
    // Fisher–Yates
    for (int i = pool.length - 1; i > 0; i--) {
      int j = rng.nextInt(i + 1);
      TetrominoType tmp = pool[i];
      pool[i] = pool[j];
      pool[j] = tmp;
    }
    Collections.addAll(bag, pool);
  }

  /**
   * Returns the next tetromino from the current bag, refilling if necessary.
   *
   * @return the next {@link TetrominoType} (never {@code null})
   */
  @Override
  public TetrominoType get() {
    if (bag.isEmpty()) {
      refill();
    }
    return bag.removeFirst();
  }
}
