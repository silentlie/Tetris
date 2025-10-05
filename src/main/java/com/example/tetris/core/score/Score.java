package com.example.tetris.core.score;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable scoreboard entry for a single game session.
 *
 * <p><strong>Components:</strong></p>
 * <ul>
 *   <li>{@code name} — player identifier as entered or derived by the client.</li>
 *   <li>{@code score} — total points earned in the session.</li>
 *   <li>{@code bestStreak} — highest consecutive-line-clear streak (or similar “combo” metric).
 *   </li>
 *   <li>{@code timeStamp} — moment the score was recorded (typically end-of-game time) in UTC.</li>
 *   <li>{@code timeElapsed} — total play duration for the session.</li>
 * </ul>
 *
 * <p><strong>Natural ordering:</strong> Instances sort by:
 * <ol>
 *   <li><b>score</b> (higher first),</li>
 *   <li>then <b>bestStreak</b> (higher first),</li>
 *   <li>then <b>timeElapsed</b> (shorter first),</li>
 *   <li>then <b>name</b> (case-insensitive A→Z),</li>
 *   <li>then <b>timeStamp</b> (earlier first).</li>
 * </ol>
 * This is suitable for leaderboard displays where ties are broken deterministically.</p>
 *
 * <p><strong>Nullability:</strong> All components are expected to be non-null. Passing a
 * {@code null} to {@link #compareTo(Score)} or constructing a {@code Score} with any {@code null}
 * component will lead to {@link NullPointerException} at comparison time.</p>
 *
 * <p><strong>Equality vs ordering:</strong> As a {@code record}, {@link #equals(Object)} compares
 * all components for exact equality. The {@link #compareTo(Score)} ordering is <em>not</em>
 * guaranteed to be consistent with {@code equals}; two different instances can compare equal by
 * ordering if all tie-breakers match.</p>
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * List<Score> top = new ArrayList<>(...);
 * Collections.sort(top); // or top.sort(null);
 * }</pre>
 */
public record Score(
    String name,
    int score,
    int bestStreak,
    Instant timeStamp,
    Duration timeElapsed
) implements Comparable<Score> {

  /**
   * Compares this score to another using the defined natural ordering: score (desc), bestStreak
   * (desc), timeElapsed (asc), name (case-insensitive asc), then timeStamp (asc).
   *
   * @param other the other score entry to compare against; must not be {@code null}
   *
   * @return a negative value if this comes before {@code other}, zero if they are tied by the
   *     ordering keys, or a positive value if this comes after {@code other}
   *
   * @throws NullPointerException if {@code other} is {@code null} or if any compared component is
   *                              {@code null}
   */
  @Override
  public int compareTo(@NotNull Score other) {
    return Comparator
        .comparingInt(Score::score).reversed()
        .thenComparingInt(Score::bestStreak).reversed()
        .thenComparing(Score::timeElapsed)
        .thenComparing(Score::name, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(Score::timeStamp)
        .compare(this, other);
  }
}
