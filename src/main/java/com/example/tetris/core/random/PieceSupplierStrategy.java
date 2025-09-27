package com.example.tetris.core.random;

import com.example.tetris.core.piece.TetrominoType;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.function.Function;
import java.util.function.Supplier;

public enum PieceSupplierStrategy {
  UNIFORM(UniformPieceSupplier::new),
  SEVEN_BAG(SevenBagPieceSupplier::new);

  private final Function<SplittableRandom, Supplier<TetrominoType>> factory;

  PieceSupplierStrategy(Function<SplittableRandom, Supplier<TetrominoType>> factory) {
    this.factory = Objects.requireNonNull(factory);
  }

  public Supplier<TetrominoType> create(SplittableRandom rng) {
    return factory.apply(rng);
  }

  public Supplier<TetrominoType> create(long seed) {
    return create(new SplittableRandom(seed));
  }
}
