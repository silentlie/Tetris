package com.example.tetris.models;

public interface LevelCurve {

  long fallIntervalNanos(long elapsedNanos);

  double levelAtNanos(long elapsedNanos);

  int startLevel();
}
