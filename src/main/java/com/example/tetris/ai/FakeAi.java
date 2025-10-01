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
 * <p><strong>Purpose:</strong> Handy for wiring tests, UI demos, and validating the
 * {@link Ai} planning lifecycle without depending on a real evaluator.</p>
 *
 * <p><strong>Controller expectations:</strong> The default controller in {@link Ai} interprets
 * {@link OpMove#opX()} as the target X aligned to the tetromino pivot (unless overridden) and
 * {@link OpMove#opRotate()} as the remaining clockwise rotation count (mod 4).</p>
 *
 * <p><strong>Determinism:</strong> Use {@link #FakeAi(long)} to make outcomes reproducible across
 * runs. The no-arg constructor uses a non-deterministic seed.</p>
 *
 * <p><strong>Thread-safety:</strong> Instances are not thread-safe. Use one {@code FakeAi}
 * per game/driver thread, which matches {@link Ai}'s intended usage.</p>
 */
public class FakeAi extends Ai {

  /**
   * Source of randomness for picking target column and rotation count.
   */
  private final RandomGenerator rng;

  /**
   * Creates a non-deterministic fake AI.
   *
   * <p>Seeds an internal {@link SplittableRandom} with a non-deterministic seed.</p>
   */
  public FakeAi() {
    this.rng = new SplittableRandom();
  }

  /**
   * Creates a deterministic fake AI with a fixed seed.
   *
   * @param seed the seed for the internal {@link SplittableRandom}
   */
  public FakeAi(long seed) {
    this.rng = new SplittableRandom(seed);
  }

  /**
   * Immediately completes with a random {@link OpMove}.
   *
   * <p><strong>Contract:</strong></p>
   * <ul>
   *   <li>{@code opX} is chosen uniformly from {@code [0, snapshot.width())}.</li>
   *   <li>{@code opRotate} is chosen uniformly from {@code [0, 4)} representing CW rotations.
   *       The controller may interpret this as “rotations remaining.”</li>
   *   <li>Returns a pre-completed {@link CompletableFuture} (no background work).</li>
   * </ul>
   *
   * @param snapshot immutable game snapshot providing board width (must not be {@code null})
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
   * <p>Idempotent.</p>
   */
  @Override
  public void shutdown() {
    // no resources to release
  }
}
