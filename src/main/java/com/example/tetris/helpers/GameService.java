package com.example.tetris.helpers;

import com.example.tetris.models.Game;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.animation.AnimationTimer;

public final class GameService extends AnimationTimer {

  private final List<Game> games = new ArrayList<>();
  private final Runnable onAllFinished;

  private long last = 0L;
  private boolean isRunning = false;

  public GameService(Runnable onAllFinished) {
    this.onAllFinished = Objects.requireNonNull(onAllFinished, "onAllFinished is null");
  }

  public void addGame(Game g) {
    games.add(g);
  }

  public boolean isRunning() {
    return isRunning;
  }

  @Override
  public void handle(long now) {
    if (last == 0L) {
      last = now;
      return;
    }
    long dt = now - last;
    last = now;
    boolean allOver = !games.isEmpty();
    for (Game g : games) {
      g.tick(dt);
      if (!g.isGameOver()) {
        allOver = false;
      }
    }
    if (allOver) {
      stop();
      onAllFinished.run();
    }
  }

  @Override
  public void start() {
    if (games.isEmpty()) {
      return;
    }
    isRunning = true;
    last = 0L;
    super.start();
  }

  @Override
  public void stop() {
    isRunning = false;
    last = 0L;
    super.stop();
  }
}
