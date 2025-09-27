package com.example.tetris.models;

import com.example.tetris.controllers.GameView;
import java.util.Objects;

public class Game {


  private final GameState state;
  private final GameView view;
  private long gravityAccNanos = 0L;

  public Game(GameState state, GameView view) {
    this.state = Objects.requireNonNull(state, "state is null");
    this.view = Objects.requireNonNull(view, "view is null");
  }

  public boolean apply(GameIntent intent) {
    if (isGameOver()) {
      return false;
    }
    if (state.apply(intent)) {
      view.markDirty();
      return true;
    }
    return false;
  }

  public boolean isGameOver() {
    return state.isGameOver();
  }

  public void tick(long dtNanos, long fallInternalNanos, int initialLevel, int currentLevel) {
    if (isGameOver()) {
      return;
    }
    gravityAccNanos += dtNanos;
    while (gravityAccNanos >= fallInternalNanos && !state.isGameOver()) {
      gravityAccNanos -= fallInternalNanos;
      if (state.apply(GameIntent.SOFT_DROP)) {
        view.markDirty();
      }
    }
    view.render(state, initialLevel, currentLevel);
  }
}
