package com.example.tetris.models;

import java.util.Objects;
import java.util.SplittableRandom;
import java.util.function.Supplier;

public final class UniformPieceSupplier implements Supplier<TetrominoType> {

  private final SplittableRandom rng;
  private final TetrominoType[] pool = TetrominoType.values();

  public UniformPieceSupplier(SplittableRandom rng) {
    this.rng = Objects.requireNonNull(rng, "rng");
  }

  @Override
  public TetrominoType get() {
    return pool[rng.nextInt(pool.length)];
  }
}
