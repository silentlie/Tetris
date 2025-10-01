package com.example.tetris.core.rule;

import static com.example.tetris.core.config.Configuration.MAX_LEVEL;

/**
 * Time-based level and gravity curve for the game.
 *
 * <p>{@code GameClock} tracks elapsed nanoseconds, derives a continuous {@code rawLevel} from
 * total play time, clamps it to {@code [1, MAX_LEVEL]}, and converts that level into a gravity
 * interval ({@code fallIntervalNanos})—the delay between automatic soft drops.</p>
 *
 * <h2>Design</h2>
 * <ul>
 *   <li><strong>Arcade feel:</strong> Level grows roughly with the square root of time; gravity
 *       accelerates exponentially with level.</li>
 *   <li><strong>Virtual time:</strong> A higher start level is modeled as if the player has already
 *       accrued some time; see {@link #startTimeNanos}.</li>
 *   <li><strong>Bounds:</strong> Levels are clamped to {@code [1, MAX_LEVEL]}; gravity has a hard
 *       lower bound of 1 ns to avoid zero/negative intervals.</li>
 * </ul>
 *
 * <p>This class is stateful and not thread-safe.</p>
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
   * <p>Choosing a higher start level pretends some time has already passed so that the growth
   * function produces approximately {@code startLevel} at {@code t = 0}.</p>
   *
   * <p>Derived from the inverse of the level-growth curve (see {@link #tick(long)}):
   * seconds {@code = 30 * (L^2 - L)} &rarr; nanoseconds {@code = 30 * NS_PER_SEC * (L*(L-1))}.</p>
   */
  public final long startTimeNanos;

  private Listener listener = new Listener() {
  };
  private long elapsedNanos;
  private double rawLevel;
  private long fallIntervalNanos;

  /**
   * Creates a clock starting at {@code startLevel}.
   *
   * <p>{@code startLevel} is clamped to {@code [1, MAX_LEVEL]}. The internal
   * {@link #startTimeNanos} is computed so that the continuous level function yields roughly
   * {@code startLevel} at time zero.</p>
   *
   * @param startLevel desired starting level (clamped to {@code [1, MAX_LEVEL]})
   */
  public GameClock(int startLevel) {
    this.startLevel = Math.max(1, Math.min(startLevel, MAX_LEVEL));
    this.rawLevel = startLevel;
    // Virtual time needed to reach startLevel on the growth curve:
    // seconds = 30 * (L^2 - L) → nanos: 30 * NS_PER_SEC * (L * (L - 1))
    this.startTimeNanos = 30 * NS_PER_SEC * ((long) startLevel * (startLevel - 1L));
    elapsedNanos = 0L;
    fallIntervalNanos = 0L;
  }

  /**
   * Sets a listener for level-up notifications (optional).
   *
   * <p>Passing {@code null} installs a no-op listener.</p>
   *
   * @param listener callback sink or {@code null} for none
   */
  public void setListener(Listener listener) {
    this.listener = (listener == null) ? new Listener() {
    } : listener;
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
   * Advance the clock by {@code dtNanos} and update level and gravity.
   *
   * <p>Negative deltas are ignored.</p>
   *
   * <h3>Level growth</h3>
   *
   * <p>Let {@code seconds = totalTimeNanos / 1e9}. The continuous level is:
   * <pre>{@code
   * rawLevel = (1 + sqrt(1 + 2 * seconds / 15)) / 2
   * }</pre>
   * clamped to {@code [1, MAX_LEVEL]}. When the floored level increases, the listener is
   * notified.</p>
   *
   * <h3>Gravity</h3>
   *
   * <p>Seconds-per-row decreases exponentially across the level range:
   * <pre>{@code
   * secondsPerRow = (1/30) ^ ((rawLevel - 1) / (MAX_LEVEL - 1))
   * fallIntervalNanos = max(1 ns, secondsPerRow * 1e9)
   * }</pre>
   *
   * @param dtNanos time delta in nanoseconds (ignored if negative)
   */
  public void tick(long dtNanos) {
    if (dtNanos < 0) {
      return;
    }
    elapsedNanos += dtNanos;

    double seconds = totalTimeNanos() / (double) NS_PER_SEC;
    int lastLevel = level();

    // Continuous level curve (sqrt-like growth)
    rawLevel = (1.0 + Math.sqrt(1.0 + 2.0 * seconds / 15.0)) / 2.0;
    rawLevel = Math.min(rawLevel, MAX_LEVEL);

    int now = level();
    if (now > lastLevel) {
      listener.onLevelUp(now);
    }

    // Exponential gravity curve across [1…MAX_LEVEL]
    double secondsPerRow = Math.pow(1.0 / 30.0, (rawLevel - 1.0) / (MAX_LEVEL - 1));
    fallIntervalNanos = (long) (secondsPerRow * 1_000_000_000.0);
    if (fallIntervalNanos <= 1L) {
      fallIntervalNanos = 1L;
    }
  }

  /**
   * Nanoseconds elapsed since construction (excludes {@link #startTimeNanos}).
   *
   * @return elapsed nanoseconds (monotonic, non-negative)
   */
  public long elapsedNanos() {
    return elapsedNanos;
  }

  /**
   * Current integer level, computed as {@code floor(rawLevel)}.
   *
   * @return level in {@code [1, MAX_LEVEL]}
   */
  public int level() {
    return (int) rawLevel;
  }

  /**
   * Current gravity interval in nanoseconds between automatic drops.
   *
   * <p>Smaller values indicate faster gravity. Always ≥ 1 ns.</p>
   *
   * @return nanoseconds between automatic row falls
   */
  public long fallIntervalNanos() {
    return fallIntervalNanos;
  }

  /**
   * Optional event callback.
   *
   * <p>Invoked synchronously from {@link #tick(long)} when the floored level increases.</p>
   */
  public interface Listener {

    /**
     * Called when the integer level increases.
     *
     * @param newLevel the new level after the increase
     */
    default void onLevelUp(int newLevel) {
    }
  }
}
