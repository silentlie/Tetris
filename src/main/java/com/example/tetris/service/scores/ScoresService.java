package com.example.tetris.service.scores;

import com.example.tetris.core.score.Score;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Thread-safe in-memory cache and persistence façade for {@link Score} leaderboards.
 *
 * <p>This abstract service keeps a sorted, in-memory list of scores (highest first using
 * {@link Score#compareTo(Score)}), while delegating storage details to subclasses via
 * {@link #loadInternal()} and {@link #saveInternal(List)}.</p>
 *
 * <h2>Design & invariants</h2>
 * <ul>
 *   <li><b>Sorted cache:</b> The internal cache is always sorted according to
 *       {@link Score}'s natural ordering.</li>
 *   <li><b>Null-safety:</b> Public methods reject {@code null} inputs with
 *       {@link NullPointerException}. Persistence hooks must not return {@code null}.</li>
 *   <li><b>Thread-safety:</b> All public methods are {@code synchronized} on {@code this};
 *       calls to {@code loadInternal}/{@code saveInternal} occur under that lock.</li>
 *   <li><b>Copy-on-read/write:</b> External callers never receive a mutable reference to the cache.
 *       Persistence receives an immutable snapshot via {@link List#copyOf(Collection)}.</li>
 * </ul>
 *
 * <h2>Subclass contract</h2>
 * <ul>
 *   <li>{@link #loadInternal()} must return a finite list (not {@code null}); it may be empty.</li>
 *   <li>{@link #saveInternal(List)} must persist the provided list as-is (order preserved),
 *       and should not retain the list reference. It may throw unchecked exceptions if persistence
 *       fails; this class does not swallow them.</li>
 * </ul>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * public final class JsonScoresService extends ScoresService {
 *   protected List<Score> loadInternal() { return List.of(); }
 *   protected void saveInternal(List<Score> scores) {}
 * }
 *
 * ScoresService svc = new JsonScoresService();
 * int index = svc.add(new Score("Jess", 12000, 6, Instant.now(), Duration.ofMinutes(8)));
 * List<Score> top = svc.all(); // sorted, unmodifiable copy
 * }</pre>
 */
public abstract class ScoresService {

  /**
   * Lazily initialized, always-sorted cache. Guarded by {@code this}.
   */
  private List<Score> cache = null;

  /**
   * Returns all scores in natural (sorted) order as an unmodifiable snapshot.
   *
   * @return an unmodifiable copy of the current leaderboard; never {@code null}
   */
  public final synchronized List<Score> all() {
    ensureLoaded();
    return List.copyOf(cache);
  }

  /**
   * Inserts a single {@link Score} into the sorted cache and persists the new state.
   *
   * <p>Insertion uses a binary search for the correct position (stable tie-breaking follows
   * {@link Score#compareTo(Score)}), then calls {@link #saveInternal(List)} with an immutable
   * snapshot of the updated list.</p>
   *
   * @param score the score to insert; must not be {@code null}
   *
   * @return the index at which the score was inserted (0-based in sorted order)
   *
   * @throws NullPointerException if {@code score} is {@code null}
   * @implNote Time complexity is O(log n) search + O(n) insertion due to list shifting.
   */
  public final synchronized int add(Score score) {
    Objects.requireNonNull(score, "score");
    ensureLoaded();
    int index = binarySearch(cache, score);
    cache.add(index, score);
    saveInternal(List.copyOf(cache));
    return index;
  }

  /**
   * Inserts all given scores into the sorted cache and persists once at the end.
   *
   * <p>Each score is inserted at its correct position using a binary search. Persistence is
   * performed a single time after all insertions to minimize I/O.</p>
   *
   * @param scores the scores to insert; the list itself and each element must not be {@code null}
   *
   * @return an array of insertion indices corresponding to each input score
   *
   * @throws NullPointerException if {@code scores} or any element is {@code null}
   * @implNote Overall complexity is O(m log n + m·n_shift) for m insertions into a list; for
   *     large batches, consider pre-merging a sorted copy to reduce shifting.
   */
  public final synchronized int[] addAll(List<Score> scores) {
    Objects.requireNonNull(scores, "scores");
    ensureLoaded();
    int[] indices = new int[scores.size()];
    cache.addAll(scores);
    Collections.sort(cache);
    for (int i = 0; i < scores.size(); i++) {
      indices[i] = binarySearch(cache, scores.get(i));
    }
    saveInternal(List.copyOf(cache));
    return indices;
  }

  /**
   * Clears all scores (both in persistence and in-memory).
   *
   * <p>Persists an empty list, then resets the cache to an empty mutable list.</p>
   */
  public final synchronized void clear() {
    saveInternal(Collections.emptyList());
    cache = new ArrayList<>();
  }

  /**
   * Returns the current number of scores.
   *
   * @return the size of the leaderboard
   */
  public final synchronized int size() {
    ensureLoaded();
    return cache.size();
  }

  /**
   * Discards the current cache and reloads from persistence.
   *
   * @return a fresh, sorted, unmodifiable snapshot of all scores
   */
  public final synchronized List<Score> reload() {
    cache = null;
    return all();
  }

  /**
   * Loads scores from persistence. Must not return {@code null}.
   *
   * @return the stored scores (any order is accepted; this class will sort them)
   */
  protected abstract List<Score> loadInternal();

  /**
   * Persists the given scores. Must not retain the reference.
   *
   * @param scores an immutable snapshot to persist; never {@code null}
   */
  protected abstract void saveInternal(List<Score> scores);

  /**
   * Ensures the cache is initialized and sorted.
   *
   * <p>Calls {@link #loadInternal()}, substitutes an empty list if it returned {@code null},
   * sorts by natural order, and copies into a mutable {@link ArrayList}.</p>
   */
  private void ensureLoaded() {
    if (cache == null) {
      List<Score> loaded = loadInternalNonNull();
      Collections.sort(loaded);
      cache = new ArrayList<>(loaded);
    }
  }

  /**
   * Binary search helper returning the correct insertion point for {@code score} in {@code sorted},
   * which must already be sorted by natural order.
   */
  private static int binarySearch(List<Score> sorted, Score score) {
    int i = Collections.binarySearch(sorted, score);
    return (i >= 0) ? i : -(i + 1);
  }

  /**
   * Loads scores and normalizes {@code null} to an empty list; validates non-nullity.
   */
  private List<Score> loadInternalNonNull() {
    return Objects.requireNonNullElse(loadInternal(), List.of());
  }
}
