package com.example.tetris.core.state;

import com.example.tetris.core.piece.TetrominoType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

/**
 * Fixed-size lookahead queue for upcoming Tetris pieces.
 *
 * <p>Backed by a {@link Deque} and a {@link Supplier} of {@link TetrominoType}, this class
 * maintains a rolling window of the next {@code size} pieces. On each {@link #popAndRefill()} call,
 * the head element is removed and a new piece from {@code source} is appended at the tail, keeping
 * the visible queue length constant.</p>
 *
 * <p>Typical sources include a uniform RNG, a 7-bag generator, or a deterministic replay stream.
 * The order exposed by {@link #peek()} is head (next to spawn) to tail (farthest preview).</p>
 *
 * <p>This class is not thread-safe.</p>
 *
 * @since 1.0
 */
public final class PieceQueue {

  /**
   * Number of preview entries to maintain (window size).
   */
  private final int size;

  /**
   * Piece source; typically RNG-backed or 7-bag.
   */
  private final Supplier<TetrominoType> source;

  /**
   * Internal deque storing the next pieces, head = next to spawn.
   */
  private final Deque<TetrominoType> deque;

  /**
   * Creates a queue with a fixed preview size and a piece supplier.
   *
   * <p>All entries are immediately filled from {@code source} up to {@code size}.</p>
   *
   * @param size   the number of upcoming pieces to maintain (must be {@code > 0})
   * @param source supplier of pieces (must not be {@code null})
   */
  public PieceQueue(int size, Supplier<TetrominoType> source) {
    this.size = size;
    this.source = java.util.Objects.requireNonNull(source);
    this.deque = new ArrayDeque<>(size);
    refillToSize();
  }

  /**
   * Removes and returns the next piece, then refills the tail from the {@code source}.
   *
   * <p>After this call, the queue still contains {@code size} elements (unless the supplier
   * throws). The returned piece is the one that was at the head before the call.</p>
   *
   * @return the next {@link TetrominoType} to spawn (never {@code null} if the supplier is
   *     well-behaved)
   *
   * @throws java.util.NoSuchElementException if the queue is unexpectedly empty
   */
  public TetrominoType popAndRefill() {
    TetrominoType t = deque.removeFirst();
    refillToSize();
    return t;
  }

  /**
   * Returns an immutable snapshot of the current preview window.
   *
   * <p>The list order is head-to-tail: index {@code 0} is the next piece that would be returned
   * by {@link #popAndRefill()}, and the last element is the farthest preview.</p>
   *
   * @return an unmodifiable view of upcoming pieces (never {@code null}, size {@code == this.size})
   */
  public List<TetrominoType> peek() {
    return List.copyOf(deque);
  }

  /**
   * Ensures the deque contains exactly {@code size} elements by appending from the supplier.
   *
   * <p>Called at construction and after each pop. If the supplier throws or returns {@code null},
   * the queue will not be fully replenished and subsequent operations may fail.</p>
   */
  private void refillToSize() {
    while (deque.size() < size) {
      deque.addLast(source.get());
    }
  }
}
