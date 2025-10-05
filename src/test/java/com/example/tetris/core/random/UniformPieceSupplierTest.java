package com.example.tetris.core.random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.tetris.core.piece.TetrominoType;
import java.util.EnumSet;
import java.util.SplittableRandom;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("UniformPieceSupplier")
class UniformPieceSupplierTest {

  // ---------- constructor ----------

  @Nested
  @DisplayName("constructor")
  class ConstructorTests {

    @Test
    @DisplayName("rejects null RNG")
    void rejects_null_rng() {
      assertThrows(NullPointerException.class, () -> new UniformPieceSupplier(null));
    }

    @Test
    @DisplayName("accepts valid RNG")
    void accepts_rng() {
      assertDoesNotThrow(() -> new UniformPieceSupplier(new SplittableRandom(123L)));
    }
  }

  @Nested
  @DisplayName("get()")
  class GetTests {

    @ParameterizedTest(name = "seed = {0}")
    @DisplayName("same seed → identical sequence")
    @ValueSource(longs = {0L, 1L, 42L, -7L, 123456789L})
    void same_seed_identical_sequence(long seed) {
      Supplier<TetrominoType> a = new UniformPieceSupplier(new SplittableRandom(seed));
      Supplier<TetrominoType> b = new UniformPieceSupplier(new SplittableRandom(seed));

      for (int i = 0; i < 1000; i++) {
        assertEquals(a.get(), b.get(), "Mismatch at draw " + i + " for seed " + seed);
      }
    }

    @ParameterizedTest(name = "seed = {0}")
    @DisplayName("different seeds → sequences usually differ")
    @ValueSource(longs = {1L, 2L, 3L, 4L, 5L})
    void different_seeds_usually_differ(long seed) {
      Supplier<TetrominoType> a = new UniformPieceSupplier(new SplittableRandom(seed));
      Supplier<TetrominoType> b = new UniformPieceSupplier(new SplittableRandom(seed + 9999));

      final int M = 200;
      boolean anyDiff = false;
      for (int i = 0; i < M; i++) {
        if (a.get() != b.get()) {
          anyDiff = true;
          break;
        }
      }
      assertTrue(anyDiff, "Sequences unexpectedly identical for " + M + " draws");
    }

    @Test
    @DisplayName("returns only valid enum values and never null")
    void only_enum_values() {
      Supplier<TetrominoType> sup = new UniformPieceSupplier(new SplittableRandom(123L));
      for (int i = 0; i < 500; i++) {
        TetrominoType t = sup.get();
        assertNotNull(t);
        assertTrue(EnumSet.allOf(TetrominoType.class).contains(t));
      }
    }
  }
}
