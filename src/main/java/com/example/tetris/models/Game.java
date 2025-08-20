package com.example.tetris.models;

public class Game {

  static final int WIDTH = 10;
  static final int HEIGHT = 20;

  private final String playerName;
  private final Integer score = 0;


  public Game(String playerName) {
    this.playerName = playerName;

  }

  public String getPlayerName() {
    return playerName;
  }

  public int getScore() {
    return score;
  }
}
