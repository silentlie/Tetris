package com.example.tetris.core.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.tetris.core.config.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GameClock")
class GameClockTest {

  @Test
  @DisplayName("startLevel is clamped to [1, MAX_LEVEL] and raw level starts at that value")
  void start_level_clamped_and_raw_initialized() {
    var c1 = new GameClock(0);
    assertEquals(1, c1.startLevel);
    assertEquals(1, c1.level());

    var c2 = new GameClock(Configuration.MAX_LEVEL + 10);
    assertEquals(Configuration.MAX_LEVEL, c2.startLevel);
    assertEquals(Configuration.MAX_LEVEL, c2.level());

    var c3 = new GameClock(5);
    assertEquals(5, c3.startLevel);
    assertEquals(5, c3.level());
  }

  @Test
  @DisplayName("fallIntervalNanos is 0 before first tick, then becomes > 0 afterwards")
  void fall_interval_requires_first_tick() {
    var c = new GameClock(1);
    assertEquals(0L, c.fallIntervalNanos(), "interval starts at 0 before first tick");
    c.tick(0);
    assertTrue(c.fallIntervalNanos() > 0L, "interval computed on first tick");
  }

  @Test
  @DisplayName("negative dt is ignored")
  void negative_dt_ignored() {
    var c = new GameClock(3);
    long beforeElapsed = c.elapsedNanos();
    int beforeLevel = c.level();
    c.tick(0);
    long beforeInterval = c.fallIntervalNanos();

    c.tick(-123);
    assertEquals(beforeElapsed, c.elapsedNanos());
    assertEquals(beforeLevel, c.level());
    assertEquals(beforeInterval, c.fallIntervalNanos());
  }

  @Test
  @DisplayName("gravity interval decreases as level increases (monotonic)")
  void gravity_monotonicity() {
    var clk = new GameClock(1);
    clk.tick(0);
    long intervalL1 = clk.fallIntervalNanos();
    assertTrue(intervalL1 > 0);

    int safety = 10_000;
    int loops = 0;
    int lastLevel = clk.level();
    while (clk.level() < Configuration.MAX_LEVEL && loops++ < safety) {
      clk.tick(500_000_000L);
      if (clk.level() > lastLevel) {
        long nowInterval = clk.fallIntervalNanos();
        assertTrue(nowInterval <= intervalL1, "interval should not increase when level rises");
        intervalL1 = nowInterval;
        lastLevel = clk.level();
      }
    }
    assertEquals(Configuration.MAX_LEVEL, clk.level(), "reached MAX_LEVEL");

    long intervalMax = clk.fallIntervalNanos();
    assertTrue(intervalMax > 0);
    assertTrue(intervalMax < 1_000_000_000L, "faster than level 1’s ~1s");
  }

  @Nested
  @DisplayName("sanity at endpoints")
  class Endpoints {

    @Test
    @DisplayName("level 1 gravity ≈ 1s per row")
    void level1_gravity() {
      var clk = new GameClock(1);
      clk.tick(0);
      long ns = clk.fallIntervalNanos();
      assertTrue(ns >= 900_000_000L && ns <= 1_100_000_000L, "≈ 1 second");
    }

    @Test
    @DisplayName("MAX_LEVEL gravity ≈ (1/30) s per row and never below 1 ns")
    void max_level_gravity() {
      var clk = new GameClock(Configuration.MAX_LEVEL);
      clk.tick(0);
      long ns = clk.fallIntervalNanos();
      assertTrue(ns >= 20_000_000L && ns <= 60_000_000L, "≈ 1/30 second");
    }
  }

  @Test
  @DisplayName("virtual time offset makes raw level ~startLevel at t=0")
  void virtual_time_consistency() {
    int L = 7;
    var clk = new GameClock(L);
    assertEquals(L, clk.level());
    clk.tick(0);
    assertEquals(L, clk.level());
  }
}
