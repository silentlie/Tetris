package com.example.tetris.core.random;

import com.example.tetris.core.piece.TetrominoType;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Shared, indexable piece stream backed by a {@link Supplier} of {@link TetrominoType}.
 *
 * <p>Multiple consumers can request the piece at a specific zero-based index via
 * {@link #getAt(int)}. The sequence is memoized: once a piece is generated for an index, it is
 * cached and reused for all future requests of that index.</p>
 *
 * <p>Typical use is to synchronize piece sequences across multiple local games or views (e.g.,
 * PvP or replay), ensuring every participant sees the same piece at the same turn index.</p>
 *
 * <p><b>Thread-safety:</b> {@link #getAt(int)} and {@link #size()} are {@code synchronized}.
 * Internal growth of the cache occurs only while holding the same monitor, so calls are safe from
 * data races. The private {@link #ensure(int)} is only invoked while the lock is held.</p>
 *
 * <p><b>Unbounded growth:</b> The cache grows to the largest index requested + 1 and never
 * shrinks. Callers should avoid requesting arbitrarily large indices if memory is a concern.</p>
 *
 * @since 1.0
 */
public final class SharedPieceSource {

  /**
   * Underlying generator of pieces (uniform, 7-bag, etc.).
   */
  private final Supplier<TetrominoType> supplier;

  /**
   * Memoized sequence; index {@code i} stores the {@code i}-th piece generated.
   */
  private final ArrayList<TetrominoType> cache = new ArrayList<>();

  /**
   * Creates a shared piece source backed by the given supplier.
   *
   * @param supplier the underlying piece supplier (must not be {@code null})
   */
  public SharedPieceSource(Supplier<TetrominoType> supplier) {
    this.supplier = Objects.requireNonNull(supplier);
  }

  /**
   * Returns the piece at the given zero-based index, generating and caching pieces as needed.
   *
   * <p>If {@code index} is equal to {@link #size()}, this call will generate exactly one new
   * piece. If {@code index} is larger than {@link #size()}, this call will generate as many pieces
   * as required to fill the gap.</p>
   *
   * <p>Example:</p>
   * <pre>{@code
   * SharedPieceSource s= new SharedPieceSource(new SevenBagPieceSupplier(new SplittableRandom(0)));
   * TetrominoType first  = s.getAt(0); // generates indices 0..0
   * TetrominoType fifth  = s.getAt(4); // generates indices 1..4
   * TetrominoType again5 = s.getAt(4); // returns cached value
   * int generated = s.size();          // == 5
   * }</pre>
   *
   * @param index zero-based piece index (must be {@code >= 0})
   *
   * @return the {@link TetrominoType} at {@code index} (never {@code null} if the supplier is
   *     well-behaved)
   *
   * @throws IndexOutOfBoundsException if {@code index < 0}
   */
  public synchronized TetrominoType getAt(int index) {
    ensure(index + 1);
    return cache.get(index);
  }

  /**
   * Ensures the cache contains at least {@code minSize} elements by pulling from the supplier.
   *
   * <p>Called only while holding this instance’s monitor.</p>
   *
   * @param minSize the minimum size to reach (must be {@code >= 0})
   */
  private void ensure(int minSize) {
    while (cache.size() < minSize) {
      cache.add(supplier.get());
    }
  }

  /**
   * Returns the number of pieces currently cached.
   *
   * <p>Equivalent to the highest requested index + 1. This value never decreases.</p>
   *
   * @return current cache size (never negative)
   */
  public synchronized int size() {
    return cache.size();
  }
}
