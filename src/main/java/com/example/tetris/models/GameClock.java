package com.example.tetris.models;

import static com.example.tetris.models.Configuration.MAX_LEVEL;

public class GameClock {

  private static final long NS_PER_SEC = 1_000_000_000L;

  public final int startLevel;
  public final long startTimeNanos;
  private long elapsedNanos;
  private double rawLevel;
  private long fallIntervalNanos;

  public GameClock(int startLevel) {
    this.startLevel = Math.max(1, Math.min(startLevel, MAX_LEVEL));
    this.rawLevel = startLevel;
    this.startTimeNanos = 30 * NS_PER_SEC * ((long) startLevel * (startLevel - 1L));
    elapsedNanos = 0L;
    fallIntervalNanos = 0L;
  }

  private long totalTimeNanos() {
    return startTimeNanos + elapsedNanos;
  }

  public void tick(long dtNanos) {
    if (dtNanos < 0) {
      return;
    }
    elapsedNanos += dtNanos;
    double seconds = totalTimeNanos() / (double) NS_PER_SEC;
    rawLevel = (1.0 + Math.sqrt(1.0 + 2.0 * seconds / 15.0)) / 2.0;
    rawLevel = Math.min(rawLevel, MAX_LEVEL);
    double secondsPerRow = Math.pow(1.0 / 30.0, (rawLevel - 1.0) / (MAX_LEVEL - 1));
    fallIntervalNanos = (long) (secondsPerRow * 1_000_000_000.0);
    if (fallIntervalNanos <= 1L) {
      fallIntervalNanos = 1L;
    }
  }

  public long elapsedNanos() {
    return elapsedNanos;
  }

  public int level() {
    return (int) rawLevel;
  }

  public long fallIntervalNanos() {
    return fallIntervalNanos;
  }
}
