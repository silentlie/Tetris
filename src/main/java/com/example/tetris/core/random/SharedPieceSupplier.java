package com.example.tetris.core.random;

import com.example.tetris.core.piece.TetrominoType;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Cursor-based view over a shared, memoized tetromino stream.
 *
 * <p>Backed by a {@link SharedPieceSource}, this supplier advances through the shared sequence
 * using a local cursor. Each call to {@link #get()} returns {@code shared.getAt(cursor)} and then
 * increments the cursor. Multiple {@code SharedPieceSupplier} instances created over the same
 * {@code SharedPieceSource} can traverse the identical sequence independently (e.g., for two local
 * players starting at different turns).</p>
 *
 * <p>Determinism derives from the underlying {@link SharedPieceSource} (and its supplier/seed).
 * This class itself maintains no RNG.</p>
 *
 * <p><b>Thread-safety:</b> This class is not synchronized. The underlying
 * {@link SharedPieceSource} guards its own mutations; however, each {@code SharedPieceSupplier}
 * instance should be used from a single thread or externally synchronized if shared.</p>
 *
 * @since 1.0
 */
public final class SharedPieceSupplier implements Supplier<TetrominoType> {

  private final SharedPieceSource shared;
  private int cursor;

  /**
   * Creates a supplier that reads from {@code shared} starting at a given index.
   *
   * <p>If {@code start} is negative, the cursor is clamped to {@code 0}.</p>
   *
   * @param shared the shared piece source (must not be {@code null})
   * @param start  the initial zero-based cursor position
   */
  public SharedPieceSupplier(SharedPieceSource shared, int start) {
    this.shared = Objects.requireNonNull(shared);
    this.cursor = Math.max(0, start);
  }

  /**
   * Creates a supplier that starts at index {@code 0}.
   *
   * @param shared the shared piece source (must not be {@code null})
   */
  public SharedPieceSupplier(SharedPieceSource shared) {
    this(shared, 0);
  }

  /**
   * Returns the next piece at the current cursor and advances the cursor by one.
   *
   * <p>Semantics:</p>
   * <ul>
   *   <li>Returns {@code shared.getAt(cursor)}.</li>
   *   <li>Post-increment: {@code cursor} becomes {@code cursor + 1} after the call.</li>
   * </ul>
   *
   * @return the next {@link TetrominoType} from the shared sequence (never {@code null} if
   *     well-behaved)
   */
  @Override
  public TetrominoType get() {
    return shared.getAt(cursor++);
  }

  /**
   * Returns the current cursor (the index that will be read on the next {@link #get()} call).
   *
   * @return the next-read index (always {@code >= 0})
   */
  public int cursor() {
    return cursor;
  }
}
