package com.example.tetris.models;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.function.Supplier;

public final class SevenBagPieceSupplier implements Supplier<TetrominoType> {

  private final SplittableRandom rng;
  private final TetrominoType[] pool = TetrominoType.values();
  private final Deque<TetrominoType> bag = new ArrayDeque<>(TetrominoType.values().length);

  public SevenBagPieceSupplier(SplittableRandom rng) {
    this.rng = Objects.requireNonNull(rng, "rng");
    refill();
  }

  private void refill() {
    // Fisher–Yates
    for (int i = pool.length - 1; i > 0; i--) {
      int j = rng.nextInt(i + 1);
      TetrominoType tmp = pool[i];
      pool[i] = pool[j];
      pool[j] = tmp;
    }
    Collections.addAll(bag, pool);
  }

  @Override
  public TetrominoType get() {
    if (bag.isEmpty()) {
      refill();
    }
    return bag.removeFirst();
  }
}
