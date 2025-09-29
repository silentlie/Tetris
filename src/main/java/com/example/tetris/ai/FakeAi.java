package com.example.tetris.ai;

import com.example.tetris.core.state.OpMove;
import com.example.tetris.core.state.PureGame;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.concurrent.CompletableFuture;

/**
 * Trivial AI that picks a random target column and rotation.
 *
 * <p>Intended for wiring, load, and demo purposes only. It does not inspect the board
 * beyond its width and makes no attempt to avoid collisions or choose good placements.</p>
 *
 * <p>Thread-safety: this class is thread-safe for typical use. {@link SplittableRandom}
 * is immutable; all state is effectively final.</p>
 */
public class FakeAi implements Ai {

  private final SplittableRandom rng;

  /**
   * Creates a non-deterministic AI seeded from a system entropy source.
   */
  public FakeAi() {
    this.rng = new SplittableRandom();
  }

  /**
   * Creates a deterministic AI, useful for tests.
   *
   * @param seed the seed for repeatable randomness
   */
  public FakeAi(long seed) {
    this.rng = new SplittableRandom(seed);
  }

  /**
   * Returns a completed future with a random {@link OpMove}.
   *
   * <p>Contract notes:
   * <ul>
   *   <li>Column is in the range [0, width - 1] (clamped to at least 1 column).</li>
   *   <li>Rotation is in the range [0, 3].</li>
   *   <li>The future is already completed; no background work is scheduled.</li>
   * </ul>
   * This keeps tests simple and the fake snappy, while still matching the async API.</p>
   *
   * @param snapshot immutable board snapshot (must not be {@code null})
   *
   * @return a <em>completed</em> future with a random move
   *
   * @throws NullPointerException if {@code snapshot} is {@code null}
   */
  @Override
  public CompletableFuture<OpMove> decideAsync(PureGame snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");
    int w = Math.max(1, snapshot.width());
    int targetX = rng.nextInt(w);   // 0..w-1
    int rot = rng.nextInt(4);       // 0..3
    return CompletableFuture.completedFuture(new OpMove(targetX, rot));
  }

  /**
   * No-op for the fake; real implementations should release resources here.
   */
  @Override
  public void shutdown() {
    // nothing to release
  }
}
