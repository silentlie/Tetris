package com.example.tetris.ai;

import com.example.tetris.core.state.OpMove;
import com.example.tetris.core.state.PureGame;
import java.util.concurrent.CompletableFuture;

/**
 * Strategy interface for computing the next best move from a game snapshot.
 *
 * <p>Implementations are typically CPU-bound and may run on a background thread.
 * The {@link #decideAsync(PureGame)} contract is:
 * <ul>
 *   <li>Never mutate the {@link PureGame} snapshot; treat it as immutable.</li>
 *   <li>Return a {@link CompletableFuture} that completes with an {@link OpMove}
 *       (target column and rotation) or {@code null} if no move is available.</li>
 *   <li>Completion may be exceptional if the evaluation fails.</li>
 *   <li>Callers may cancel the returned future when the snapshot becomes stale.</li>
 * </ul>
 * </p>
 *
 * <p>Lifecycle: callers should invoke {@link #shutdown()} when the AI is no longer needed
 * to release any thread pools or native resources.</p>
 */
public interface Ai {

  /**
   * Asynchronously decides a move for the given immutable game snapshot.
   *
   * <p>Implementations should be side effect free with respect to the supplied snapshot
   * and should return promptly by performing work off the caller thread.</p>
   *
   * @param snapshot a self-contained, immutable view of the board and active piece
   *
   * @return a future that completes with an {@link OpMove} (or {@code null} if no plan)
   *
   * @throws NullPointerException if {@code snapshot} is {@code null} (implementations may enforce)
   */
  CompletableFuture<OpMove> decideAsync(PureGame snapshot);

  /**
   * Releases resources held by the AI (e.g., thread pools, models, native handles).
   *
   * <p>After shutdown, further calls to {@link #decideAsync(PureGame)} may throw or
   * return futures that complete exceptionally, per implementation.</p>
   */
  void shutdown();
}