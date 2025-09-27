package com.example.tetris.models;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;

public final class SharedPieceSource {

  private final Supplier<TetrominoType> supplier;
  private final ArrayList<TetrominoType> cache = new ArrayList<>();

  public SharedPieceSource(Supplier<TetrominoType> supplier) {
    this.supplier = Objects.requireNonNull(supplier);
  }

  public synchronized TetrominoType getAt(int index) {
    ensure(index + 1);
    return cache.get(index);
  }

  private void ensure(int minSize) {
    while (cache.size() < minSize) {
      cache.add(supplier.get());
    }
  }

  public synchronized int size() {
    return cache.size();
  }
}