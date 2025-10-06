package com.example.tetris.core.random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.tetris.core.piece.TetrominoType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SevenBagPieceSupplier")
class SevenBagPieceSupplierTest {

  private static List<TetrominoType> drawN(SevenBagPieceSupplier sup, int n) {
    List<TetrominoType> out = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      out.add(sup.get());
    }
    return out;
  }

  private static void perm7(List<TetrominoType> chunk) {
    assertEquals(7, chunk.size(), "Chunk must have size 7");
    EnumSet<TetrominoType> seen = EnumSet.noneOf(TetrominoType.class);
    seen.addAll(chunk);
    assertEquals(EnumSet.allOf(TetrominoType.class), seen,
        "Chunk must contain each tetromino exactly once");
  }

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("rejects null RNG")
    void rejects_null_rng() {
      assertThrows(NullPointerException.class, () -> new SevenBagPieceSupplier(null));
    }

    @Test
    @DisplayName("initial bag available immediately (first 7 are a permutation)")
    void initial_bag_ready() {
      SevenBagPieceSupplier sup = new SevenBagPieceSupplier(new SplittableRandom(123));
      perm7(drawN(sup, 7));
    }
  }

  @Nested
  @DisplayName("bag invariants")
  class BagInvariants {

    @ParameterizedTest(name = "{0} bag(s)")
    @DisplayName("each bag of 7 draws is a permutation of all types")
    @ValueSource(ints = {1, 2, 3, 10})
    void each_bag_is_permutation(int bagCount) {
      SevenBagPieceSupplier sup = new SevenBagPieceSupplier(new SplittableRandom(42));
      List<TetrominoType> draws = drawN(sup, bagCount * 7);
      for (int i = 0; i < bagCount; i++) {
        perm7(draws.subList(i * 7, (i + 1) * 7));
      }
    }

    @Test
    @DisplayName("refill boundary works: 7 draws exhaust bag; next draw starts fresh bag")
    void refill_boundary() {
      SevenBagPieceSupplier sup = new SevenBagPieceSupplier(new SplittableRandom(7));
      perm7(drawN(sup, 7));
      perm7(drawN(sup, 7));
    }

    @Test
    @DisplayName("after K complete bags, each type appears exactly K times")
    void exact_counts_across_complete_bags() {
      int bags = 50; // arbitrary
      SevenBagPieceSupplier sup = new SevenBagPieceSupplier(new SplittableRandom(2025));
      List<TetrominoType> draws = drawN(sup, bags * 7);

      Map<TetrominoType, Integer> counts = new EnumMap<>(TetrominoType.class);
      for (TetrominoType t : TetrominoType.values()) {
        counts.put(t, 0);
      }
      for (TetrominoType t : draws) {
        counts.merge(t, 1, Integer::sum);
      }

      for (TetrominoType t : TetrominoType.values()) {
        assertEquals(bags, counts.get(t), "Each type should appear exactly " + bags + " times");
      }
    }
  }

  @Nested
  @DisplayName("RNG / reproducibility")
  class RngReproduce {

    @ParameterizedTest(name = "seed = {0}")
    @DisplayName("same seed → identical sequence")
    @ValueSource(longs = {0L, 1L, 42L, -7L, 123456789L})
    void same_seed_identical_sequence(long seed) {
      SevenBagPieceSupplier a = new SevenBagPieceSupplier(new SplittableRandom(seed));
      SevenBagPieceSupplier b = new SevenBagPieceSupplier(new SplittableRandom(seed));
      assertEquals(drawN(a, 100), drawN(b, 100));
    }

    @ParameterizedTest(name = "seed = {0}")
    @DisplayName("different seeds → sequences usually differ")
    @ValueSource(longs = {1L, 2L, 3L, 4L, 5L})
    void different_seeds_usually_differ(long seed) {
      SevenBagPieceSupplier a = new SevenBagPieceSupplier(new SplittableRandom(seed));
      SevenBagPieceSupplier b = new SevenBagPieceSupplier(new SplittableRandom(seed + 9999));

      List<TetrominoType> da = drawN(a, 49);
      List<TetrominoType> db = drawN(b, 49);

      boolean anyDiff = false;
      for (int i = 0; i < da.size(); i++) {
        if (da.get(i) != db.get(i)) {
          anyDiff = true;
          break;
        }
      }
      assertTrue(anyDiff, "Sequences unexpectedly identical for 49 draws with different seeds");
    }
  }

  // ---- sanity --------------------------------------------------------------

  @Nested
  @DisplayName("sanity")
  class Sanity {

    @Test
    @DisplayName("never returns null; values are always valid enum constants")
    void never_null() {
      SevenBagPieceSupplier sup = new SevenBagPieceSupplier(new SplittableRandom(987654321L));
      for (int i = 0; i < 500; i++) {
        TetrominoType t = sup.get();
        assertNotNull(t);
        assertTrue(EnumSet.allOf(TetrominoType.class).contains(t));
      }
    }
  }
}
