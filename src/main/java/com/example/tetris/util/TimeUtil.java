package com.example.tetris.util;

import java.time.Duration;

public final class TimeUtil {

  private TimeUtil() {
    throw new AssertionError("No instances");
  }

  public static String formatElapsedNanos(long nanosTotal) {
    if (nanosTotal < 0L) {
      nanosTotal = 0L;
    }

    Duration d = Duration.ofNanos(nanosTotal);
    return formatElapsed(d);
  }

  public static String formatElapsed(Duration d) {
    long hours = d.toHours();
    int minutes = d.toMinutesPart();   // Java 9+
    int seconds = d.toSecondsPart();
    int nanos = d.toNanosPart();
    int nanos7 = nanos / 100;
    if (hours > 0) {
      return String.format("%dh%02dm%02d.%07ds", hours, minutes, seconds, nanos7);
    } else if (minutes > 0) {
      return String.format("%dm%02d.%07ds", minutes, seconds, nanos7);
    } else {
      return String.format("%d.%07ds", seconds, nanos7);
    }
  }
}