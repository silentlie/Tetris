package com.example.tetris.util;

import java.time.Duration;

/**
 * Time formatting helpers for human-readable elapsed durations.
 *
 * <p>Formats elapsed time with a compact suffix and fixed-width fractional seconds
 * (7 digits = 100 ns resolution). Examples:
 * <ul>
 *   <li>{@code 3.0000000s}</li>
 *   <li>{@code 42.1234567s}</li>
 *   <li>{@code 5m07.0000000s}</li>
 *   <li>{@code 2h03m04.5000000s}</li>
 * </ul>
 *
 * <p>Negative inputs are clamped to zero.</p>
 *
 * <p>Java 9+ is required for {@link Duration#toMinutesPart()}, {@link Duration#toSecondsPart()},
 * and {@link Duration#toNanosPart()}.</p>
 */
public final class TimeUtil {

  /**
   * Not instantiable.
   */
  private TimeUtil() {
    throw new AssertionError("No instances");
  }

  /**
   * Formats an elapsed time given in nanoseconds.
   *
   * <p>Negative values are treated as {@code 0}.</p>
   *
   * @param nanosTotal total elapsed nanoseconds
   *
   * @return a human-readable string such as {@code "42.1234567s"}, {@code "5m07.0000000s"}, or
   *     {@code "2h03m04.5000000s"}
   */
  public static String formatElapsedNanos(long nanosTotal) {
    if (nanosTotal < 0L) {
      nanosTotal = 0L;
    }

    Duration d = Duration.ofNanos(nanosTotal);
    return formatElapsed(d);
  }

  /**
   * Formats a {@link Duration} using hours/minutes when non-zero and seconds with 7 fractional
   * digits (100 ns precision).
   *
   * <p>Layout rules:</p>
   * <ul>
   *   <li>If hours &gt; 0: {@code HhMMmSS.fffffff}s</li>
   *   <li>Else if minutes &gt; 0: {@code MmSS.fffffff}s</li>
   *   <li>Else: {@code S.fffffff}s</li>
   * </ul>
   *
   * @param d duration to format (must not be {@code null})
   *
   * @return formatted string with a trailing {@code s} for seconds
   */
  public static String formatElapsed(Duration d) {
    long hours = d.toHours();
    int minutes = d.toMinutesPart();   // Java 9+
    int seconds = d.toSecondsPart();
    int nanos = d.toNanosPart();
    int nanos7 = nanos / 100; // 7 digits (truncate to 100 ns)
    if (hours > 0) {
      return String.format("%dh%02dm%02d.%07ds", hours, minutes, seconds, nanos7);
    } else if (minutes > 0) {
      return String.format("%dm%02d.%07ds", minutes, seconds, nanos7);
    } else {
      return String.format("%d.%07ds", seconds, nanos7);
    }
  }
}
