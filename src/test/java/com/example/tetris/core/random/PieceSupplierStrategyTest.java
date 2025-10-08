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
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("PieceSupplierStrategy")
class PieceSupplierStrategyTest {

  private static List<TetrominoType> draw(Supplier<TetrominoType> sup, int n) {
    List<TetrominoType> out = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      out.add(sup.get());
    }
    return out;
  }

  private static void perm7(List<TetrominoType> seven) {
    assertEquals(7, seven.size(), "Chunk must be size 7");
    EnumSet<TetrominoType> seen = EnumSet.noneOf(TetrominoType.class);
    seen.addAll(seven);
    assertEquals(EnumSet.allOf(TetrominoType.class), seen,
        "Chunk must contain each tetromino exactly once");
  }

  @Nested
  @DisplayName("create(SplittableRandom)")
  class CreateWithRng {

    @Test
    @DisplayName("null RNG → NullPointerException (propagated by underlying supplier)")
    void null_rng() {
      assertThrows(NullPointerException.class, () -> PieceSupplierStrategy.UNIFORM.create(null));
      assertThrows(NullPointerException.class, () -> PieceSupplierStrategy.SEVEN_BAG.create(null));
    }

    @ParameterizedTest
    @EnumSource(PieceSupplierStrategy.class)
    @DisplayName("same RNG seed → identical sequence for given strategy")
    void same_seed(PieceSupplierStrategy strategy) {
      SplittableRandom rngA = new SplittableRandom(12345L);
      SplittableRandom rngB = new SplittableRandom(12345L);

      Supplier<TetrominoType> a = strategy.create(rngA);
      Supplier<TetrominoType> b = strategy.create(rngB);

      assertEquals(draw(a, 100), draw(b, 100));
    }

    @ParameterizedTest
    @EnumSource(PieceSupplierStrategy.class)
    @DisplayName("distinct RNG splits → sequences usually differ")
    void split_differs(PieceSupplierStrategy strategy) {
      SplittableRandom root = new SplittableRandom(2025L);
      SplittableRandom r1 = root.split();
      SplittableRandom r2 = root.split();

      Supplier<TetrominoType> a = strategy.create(r1);
      Supplier<TetrominoType> b = strategy.create(r2);

      List<TetrominoType> da = draw(a, 120);
      List<TetrominoType> db = draw(b, 120);

      boolean anyDiff = false;
      for (int i = 0; i < da.size(); i++) {
        if (da.get(i) != db.get(i)) {
          anyDiff = true;
          break;
        }
      }
      assertTrue(anyDiff, "Sequences unexpectedly identical from distinct splits");
    }
  }

  @Nested
  @DisplayName("create(long seed)")
  class CreateWithSeed {

    @ParameterizedTest(name = "seed={0}")
    @ValueSource(longs = {0L, 1L, 42L, -7L, 123456789L})
    @DisplayName("same seed → identical sequence per strategy")
    void reproducible(long seed) {
      for (PieceSupplierStrategy strategy : PieceSupplierStrategy.values()) {
        Supplier<TetrominoType> a = strategy.create(seed);
        Supplier<TetrominoType> b = strategy.create(seed);
        assertEquals(draw(a, 200), draw(b, 200), "Mismatch for " + strategy + " with seed " + seed);
      }
    }

    @ParameterizedTest(name = "seed={0}")
    @ValueSource(longs = {1L, 2L, 3L, 4L, 5L})
    @DisplayName("different seeds → sequences usually differ")
    void different_seeds(long seed) {
      for (PieceSupplierStrategy strategy : PieceSupplierStrategy.values()) {
        Supplier<TetrominoType> a = strategy.create(seed);
        Supplier<TetrominoType> b = strategy.create(seed + 9999);

        List<TetrominoType> da = draw(a, 140);
        List<TetrominoType> db = draw(b, 140);

        boolean anyDiff = false;
        for (int i = 0; i < da.size(); i++) {
          if (da.get(i) != db.get(i)) {
            anyDiff = true;
            break;
          }
        }
        assertTrue(anyDiff, "Unexpected identical sequences for " + strategy);
      }
    }
  }

  @Nested
  @DisplayName("strategy semantics")
  class StrategySemantics {

    @Test
    @DisplayName("SEVEN_BAG: every group of 7 is a permutation; counts balance across full bags")
    void seven_bag_invariants() {
      Supplier<TetrominoType> sup = PieceSupplierStrategy.SEVEN_BAG.create(777L);

      List<TetrominoType> draws = draw(sup, 5 * 7);
      for (int i = 0; i < 5; i++) {
        perm7(draws.subList(i * 7, (i + 1) * 7));
      }

      Map<TetrominoType, Integer> counts = new EnumMap<>(TetrominoType.class);
      for (TetrominoType t : TetrominoType.values()) {
        counts.put(t, 0);
      }
      for (TetrominoType t : draws) {
        counts.merge(t, 1, Integer::sum);
      }
      for (TetrominoType t : TetrominoType.values()) {
        assertEquals(5, counts.get(t));
      }
    }

    @Test
    @DisplayName("UNIFORM: values are valid, non-null, and show reasonable diversity quickly")
    void uniform_sanity() {
      Supplier<TetrominoType> sup = PieceSupplierStrategy.UNIFORM.create(13579L);
      EnumSet<TetrominoType> seen = EnumSet.noneOf(TetrominoType.class);

      for (int i = 0; i < 100; i++) {
        TetrominoType t = sup.get();
        assertNotNull(t);
        assertTrue(EnumSet.allOf(TetrominoType.class).contains(t));
        seen.add(t);
      }
      assertTrue(seen.size() >= 3, "Suspiciously low diversity: " + seen.size());
    }
  }
}
