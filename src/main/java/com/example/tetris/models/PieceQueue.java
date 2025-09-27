package com.example.tetris.models;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

public final class PieceQueue {

  private final int size;
  private final Supplier<TetrominoType> source;
  private final Deque<TetrominoType> deque;

  public PieceQueue(int size, Supplier<TetrominoType> source) {
    if (size <= 0) {
      throw new IllegalArgumentException("size must be > 0");
    }
    this.size = size;
    this.source = java.util.Objects.requireNonNull(source);
    this.deque = new ArrayDeque<>(size);
    refillToSize();
  }

  public TetrominoType popAndRefill() {
    TetrominoType t = deque.removeFirst();
    refillToSize();
    return t;
  }

  public List<TetrominoType> peek() {
    return List.copyOf(deque);
  }

  private void refillToSize() {
    while (deque.size() < size) {
      deque.addLast(source.get());
    }
  }
}
