package com.example.tetris.core.rule;

import com.example.tetris.core.state.GameIntent;
import com.example.tetris.core.state.GameState;
import com.example.tetris.ui.view.GameView;
import java.util.Objects;

public class Game {


  private final GameState state;
  private final GameView view;
  private final GameClock clock;
  private long gravityAccNanos = 0L;

  public Game(GameState state, GameView view, GameClock clock) {
    this.state = Objects.requireNonNull(state, "state is null");
    this.view = Objects.requireNonNull(view, "views is null");
    this.clock = Objects.requireNonNull(clock, "clock is null");
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

  public void tick(long dtNanos) {
    if (!isGameOver()) {
      clock.tick(dtNanos);
      gravityAccNanos += dtNanos;
      while (gravityAccNanos >= clock.fallIntervalNanos() && !state.isGameOver()) {
        gravityAccNanos -= clock.fallIntervalNanos();
        if (state.apply(GameIntent.SOFT_DROP)) {
          view.markDirty();
        }
      }
    }
    view.render(state, clock);
  }
}
