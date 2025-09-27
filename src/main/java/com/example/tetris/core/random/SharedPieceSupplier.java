package com.example.tetris.core.random;

import com.example.tetris.core.piece.TetrominoType;
import java.util.Objects;
import java.util.function.Supplier;

public final class SharedPieceSupplier implements Supplier<TetrominoType> {

  private final SharedPieceSource shared;
  private int cursor;

  public SharedPieceSupplier(SharedPieceSource shared, int start) {
    this.shared = Objects.requireNonNull(shared);
    this.cursor = Math.max(0, start);
  }

  public SharedPieceSupplier(SharedPieceSource shared) {
    this(shared, 0);
  }

  @Override
  public TetrominoType get() {
    return shared.getAt(cursor++);
  }

  public int cursor() {
    return cursor;
  }
}
