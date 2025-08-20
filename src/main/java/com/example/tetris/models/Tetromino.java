package com.example.tetris.models;

public class Tetromino {

  private final TetrominoType type;
  private final Rotation rotation;
  private final Cell position;

  public Tetromino(Cell position) {
    type = TetrominoType.randomType();
    rotation = Rotation.randomRotation();
    this.position = position;
  }

  
}
