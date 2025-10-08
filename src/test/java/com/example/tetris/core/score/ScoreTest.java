package com.example.tetris.core.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScoreTest {

  private static Score s(String name, int score, int streak, long elapsedSec, long epochSec) {
    return new Score(
        name,
        score,
        streak,
        Instant.ofEpochSecond(epochSec),
        Duration.ofSeconds(elapsedSec));
  }

  @Test
  @DisplayName("Primary: score desc (higher first)")
  void scoreDesc() {
    Score a = s("A", 2000, 1, 60, 100);
    Score b = s("B", 1500, 20, 10, 100);
    assertTrue(a.compareTo(b) < 0);
    assertTrue(b.compareTo(a) > 0);
  }

  @Test
  @DisplayName("Tie #1: bestStreak desc when score ties")
  void streakDescOnScoreTie() {
    Score a = s("A", 2000, 5, 60, 100);
    Score b = s("B", 2000, 3, 10, 100);
    assertTrue(a.compareTo(b) < 0);
  }

  @Test
  @DisplayName("Tie #2: elapsed asc when score & streak tie")
  void elapsedAscOnScoreStreakTie() {
    Score slow = s("A", 1000, 4, 300, 100);
    Score fast = s("B", 1000, 4, 120, 100);
    assertTrue(fast.compareTo(slow) < 0);
  }

  @Test
  @DisplayName("Tie #3: name A→Z (case-insensitive) when score, streak, elapsed tie")
  void nameCaseInsensitiveAsc() {
    Score alpha = s("Alpha", 800, 2, 50, 100);
    Score beta = s("beta", 800, 2, 50, 100);
    assertTrue(alpha.compareTo(beta) < 0);
  }

  @Test
  @DisplayName("Tie #4: timeStamp asc when all else ties")
  void timeStampAsc() {
    Score early = s("Same", 500, 1, 30, 100);
    Score late = s("Same", 500, 1, 30, 200);
    assertTrue(early.compareTo(late) < 0);
  }

  @Test
  @DisplayName("End-to-end: sorting yields expected total order")
  void totalOrder() {
    List<Score> scores = new ArrayList<>(List.of(
        s("delta", 1200, 5, 100, 100),
        s("Echo", 1200, 5, 90, 90),
        s("bravo", 1500, 3, 120, 110),
        s("alpha", 1500, 6, 180, 200),
        s("CHARLIE", 1500, 6, 180, 150)
    ));
    Collections.sort(scores);
    assertEquals("CHARLIE", scores.get(0).name());
    assertEquals("alpha", scores.get(1).name());
    assertEquals("bravo", scores.get(2).name());
    assertEquals("Echo", scores.get(3).name());
    assertEquals("delta", scores.get(4).name());
  }

  @Test
  @DisplayName("Ordering ties can differ from equals() (case-insensitive name)")
  void orderTieNotEquals() {
    Score lower = s("alice", 100, 2, 30, 1234);
    Score upper = s("ALICE", 100, 2, 30, 1234);
    assertEquals(0, lower.compareTo(upper));
    assertNotEquals(lower, upper);
  }

  @Test
  @DisplayName("Transitivity sanity check")
  void transitivity() {
    Score s1 = s("a", 1000, 5, 60, 100);
    Score s2 = s("b", 900, 5, 60, 100);
    Score s3 = s("c", 800, 5, 60, 100);
    assertTrue(s1.compareTo(s2) < 0);
    assertTrue(s2.compareTo(s3) < 0);
    assertTrue(s1.compareTo(s3) < 0);
  }
}
