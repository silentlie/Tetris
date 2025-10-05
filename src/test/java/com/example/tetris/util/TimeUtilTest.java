package com.example.tetris.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("TimeUtil — formatting of elapsed durations")
class TimeUtilTest {

  private static long s(long seconds) {
    return seconds * 1_000_000_000L;
  }

  static Stream<Arguments> nanosCases() {
    return Stream.of(
        arguments(named("Zero → 0.0000000s", new Case(0L, "0.0000000s"))),
        arguments(named("Negative clamped → 0.0000000s", new Case(-1L, "0.0000000s"))),
        arguments(named("0.123456789s → 0.1234567s", new Case(123_456_789L, "0.1234567s"))),
        arguments(named("59.9999999s stays seconds layout",
            new Case(s(59) + 999_999_900L, "59.9999999s"))),
        arguments(named("Exactly 1 min", new Case(s(60), "1m00.0000000s"))),
        arguments(named("5m07s zero-padded", new Case(s(5 * 60 + 7), "5m07.0000000s"))),
        arguments(named("Truncation at 100ns", new Case(s(1) + 999_999_999L, "1.9999999s")))
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("nanosCases")
  @DisplayName("formatElapsedNanos: 7 fractional digits with truncation")
  void nanos_formats(Case c) {
    assertEquals(c.expected, TimeUtil.formatElapsedNanos(c.nanos));
  }

  static Stream<Arguments> durationCases() {
    return Stream.of(
        arguments(named("Duration.ZERO → 0.0000000s",
            new DurCase(Duration.ZERO, "0.0000000s"))),
        arguments(named("Seconds-only layout",
            new DurCase(Duration.ofSeconds(42), "42.0000000s"))),
        arguments(named("Minutes layout",
            new DurCase(Duration.ofMinutes(12).plusSeconds(34).plusNanos(567_890_123L),
                "12m34.5678901s"))),
        arguments(named("Hours layout",
            new DurCase(Duration.ofHours(1), "1h00m00.0000000s")))
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("durationCases")
  @DisplayName("formatElapsed(Duration): hours/minutes/seconds branching + truncation")
  void duration_formats(DurCase c) {
    assertEquals(c.expected, TimeUtil.formatElapsed(c.duration));
  }

  @Test
  @DisplayName("Boundary below minute: 59.99999995s truncates to 59.9999999s (no rounding)")
  void boundary_truncates() {
    long nanos = s(59) + 999_999_950L;
    assertEquals("59.9999999s", TimeUtil.formatElapsedNanos(nanos));
  }

  private record Case(long nanos, String expected) {

  }

  private record DurCase(Duration duration, String expected) {

  }
}
