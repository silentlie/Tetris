package com.example.tetris.ai;

import com.example.tetris.core.state.OpMove;
import com.example.tetris.core.state.PureGame;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.concurrent.CompletableFuture;
import java.util.random.RandomGenerator;

/**
 * A trivial AI that chooses a random horizontal target and a random rotation count.
 *
 * <p><strong>Purpose:</strong> Useful for wiring tests, UI demos, and validating the
 * {@link Ai} template lifecycle without relying on a real evaluator.</p>
 *
 * <p><strong>Determinism:</strong> Use {@link #FakeAi(long)} to make outcomes reproducible
 * across runs; the no-arg constructor uses a non-deterministic seed.</p>
 */
public class FakeAi extends Ai {

  /**
   * Source of randomness for picking target column and rotation count.
   */
  private final RandomGenerator rng;

  /**
   * Creates a non-deterministic fake AI.
   */
  public FakeAi() {
    this.rng = new SplittableRandom();
  }

  /**
   * Creates a deterministic fake AI with a fixed seed.
   *
   * @param seed the seed for the {@link SplittableRandom}
   */
  public FakeAi(long seed) {
    this.rng = new SplittableRandom(seed);
  }

  /**
   * Immediately completes with a random {@link OpMove}.
   *
   * <p>Contract details:
   * <ul>
   *   <li>{@code opX} is chosen uniformly from {@code [0, snapshot.width())}.</li>
   *   <li>{@code opRotate} is chosen uniformly from {@code [0, 4)} representing CW rotations.</li>
   *   <li>Returns a pre-completed {@link CompletableFuture} (no background work).</li>
   * </ul>
   * </p>
   *
   * @param snapshot immutable game snapshot providing board width
   *
   * @return a completed future containing a random plan
   *
   * @throws NullPointerException if {@code snapshot} is {@code null}
   */
  @Override
  protected CompletableFuture<OpMove> submit(PureGame snapshot) {
    Objects.requireNonNull(snapshot, "snapshot is null");
    int x = rng.nextInt(snapshot.width());
    int r = rng.nextInt(4);
    return CompletableFuture.completedFuture(new OpMove(x, r));
  }

  /**
   * No-op shutdown; {@link FakeAi} owns no external resources.
   *
   * <p>Method is idempotent.</p>
   */
  @Override
  public void shutdown() {
    // no resources to release
  }
}
