package com.example.tetris.helpers;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;

public class ResizableCanvas extends Region {

  protected final Canvas canvas = new Canvas();
  protected final GraphicsContext gc = canvas.getGraphicsContext2D();
  protected boolean dirty = true;

  public ResizableCanvas() {
    getChildren().add(canvas);
    canvas.widthProperty().bind(widthProperty());
    canvas.heightProperty().bind(heightProperty());
    widthProperty().addListener((obs, oldVal, newVal) -> markDirty());
    heightProperty().addListener((obs, oldVal, newVal) -> markDirty());
    gc.setImageSmoothing(false);
  }

  public void markDirty() {
    dirty = true;
  }
}
