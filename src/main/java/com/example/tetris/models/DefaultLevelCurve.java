package com.example.tetris.models;

import static com.example.tetris.models.Configuration.MAX_LEVEL;

public final class DefaultLevelCurve implements LevelCurve {

  public final int startLevel;
  private long cacheNanos;
  private double cacheLevel;
  private long cacheFallIntervalNanos;

  public DefaultLevelCurve(int startLevel) {
    this.startLevel = Math.max(1, Math.min(MAX_LEVEL, startLevel));
  }

  @Override
  public int startLevel() {
    return startLevel;
  }

  @Override
  public double levelAtNanos(long elapsedNanos) {
    if (elapsedNanos == cacheNanos) {
      return cacheLevel;
    }
    fallIntervalNanos(elapsedNanos);
    return cacheLevel;
  }

  @Override
  public long fallIntervalNanos(long elapsedNanos) {
    if (elapsedNanos == cacheNanos) {
      return cacheFallIntervalNanos;
    }
    cacheNanos = elapsedNanos;
    // Your existing curve, centralized:
    double initialTimeSeconds = 30.0 * (startLevel * startLevel - startLevel);
    double totalTime = initialTimeSeconds + elapsedNanos / 1_000_000_000.0;
    cacheLevel = (1.0 + Math.sqrt(1.0 + (2.0 * totalTime) / 15.0)) / 2.0;
    double clamped = Math.min(cacheLevel, MAX_LEVEL);
    double secondsPerRow = Math.pow(1.0 / 30.0, (clamped - 1.0) / (MAX_LEVEL - 1));
    cacheFallIntervalNanos = (long) (secondsPerRow * 1_000_000_000.0);
    return cacheFallIntervalNanos;
  }
}
