package com.example.tetris.core.rule;

import static com.example.tetris.core.config.Configuration.MAX_LEVEL;

/**
 * Time-based level and gravity curve for the game.
 *
 * <p>{@code GameClock} tracks elapsed nanoseconds, derives a continuous {@code rawLevel} from
 * total play time, clamps it to {@code [1, MAX_LEVEL]}, and converts that level into a gravity
 * interval ({@code fallIntervalNanos})—the delay between automatic “soft” drops.</p>
 *
 * <p>The curve is designed to feel arcade-like: level grows roughly with the square root of time,
 * while seconds-per-row decreases exponentially across levels. The specific functions are noted in
 * {@link #tick(long)}.</p>
 *
 * <p>This class is stateful but not thread-safe.</p>
 */
public class GameClock {

  private static final long NS_PER_SEC = 1_000_000_000L;

  /**
   * Initial level requested by the caller, clamped to {@code [1, MAX_LEVEL]}.
   */
  public final int startLevel;

  /**
   * Initial “virtual time” in nanoseconds implied by {@link #startLevel}.
   *
   * <p>Choosing a higher start level pretends you have already played for some time, so the
   * growth function produces the requested level at {@code t = 0}.</p>
   */
  public final long startTimeNanos;

  private long elapsedNanos;
  private double rawLevel;
  private long fallIntervalNanos;

  /**
   * Creates a clock starting at {@code startLevel}.
   *
   * <p>{@code startLevel} is clamped to {@code [1, MAX_LEVEL]}. The internal
   * {@link #startTimeNanos} is set so that the level function (see {@link #tick(long)}) yields
   * approximately {@code startLevel} at time zero.</p>
   *
   * @param startLevel desired starting level
   */
  public GameClock(int startLevel) {
    this.startLevel = Math.max(1, Math.min(startLevel, MAX_LEVEL));
    this.rawLevel = startLevel;
    // Virtual time that would have been needed to reach startLevel on the growth curve:
    // seconds = 30 * (L^2 - L)  → in nanos: 30 * NS_PER_SEC * (L * (L - 1))
    this.startTimeNanos = 30 * NS_PER_SEC * ((long) startLevel * (startLevel - 1L));
    elapsedNanos = 0L;
    fallIntervalNanos = 0L;
  }

  /**
   * Returns total “played” time in nanoseconds including the virtual offset from the start level.
   *
   * @return cumulative time in nanoseconds
   */
  private long totalTimeNanos() {
    return startTimeNanos + elapsedNanos;
  }

  /**
   * Advances the clock by {@code dtNanos} and updates the level and gravity interval.
   *
   * <p>Negative deltas are ignored.</p>
   *
   * <p><b>Level growth:</b> Let {@code seconds = totalTimeNanos / 1e9}. The continuous level is:
   * {@code rawLevel = (1 + sqrt(1 + 2 * seconds / 15)) / 2}, then clamped to
   * {@code [1, MAX_LEVEL]}.</p>
   *
   * <p><b>Gravity:</b> Seconds-per-row decreases exponentially across the level range:
   * {@code secondsPerRow = (1/30) ^ ((rawLevel - 1) / (MAX_LEVEL - 1))}. The corresponding
   * {@code fallIntervalNanos} is {@code secondsPerRow * 1e9}, with a hard lower bound of 1 ns.</p>
   *
   * @param dtNanos time delta in nanoseconds (ignored if negative)
   */
  public void tick(long dtNanos) {
    if (dtNanos < 0) {
      return;
    }
    elapsedNanos += dtNanos;

    double seconds = totalTimeNanos() / (double) NS_PER_SEC;

    // Continuous level curve (sqrt-like growth)
    rawLevel = (1.0 + Math.sqrt(1.0 + 2.0 * seconds / 15.0)) / 2.0;
    rawLevel = Math.min(rawLevel, MAX_LEVEL);

    // Exponential gravity curve across [1…MAX_LEVEL]
    double secondsPerRow = Math.pow(1.0 / 30.0, (rawLevel - 1.0) / (MAX_LEVEL - 1));
    fallIntervalNanos = (long) (secondsPerRow * 1_000_000_000.0);
    if (fallIntervalNanos <= 1L) {
      fallIntervalNanos = 1L;
    }
  }

  /**
   * Returns nanoseconds elapsed since construction (not including {@link #startTimeNanos}).
   *
   * @return elapsed nanoseconds (monotonic, non-negative)
   */
  public long elapsedNanos() {
    return elapsedNanos;
  }

  /**
   * Returns the current integer level (floored from the continuous {@code rawLevel}).
   *
   * @return current level in {@code [1, MAX_LEVEL]}
   */
  public int level() {
    return (int) rawLevel;
  }

  /**
   * Returns the current gravity interval in nanoseconds between automatic drops.
   *
   * <p>Smaller values indicate faster gravity. This value is always ≥ 1 ns.</p>
   *
   * @return nanoseconds between automatic row falls
   */
  public long fallIntervalNanos() {
    return fallIntervalNanos;
  }
}
