package com.example.tetris.ui.canvas;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;

/**
 * Region-backed, resizable wrapper around a JavaFX {@link Canvas}.
 *
 * <p>The internal {@link Canvas} is automatically bound to this region’s width and height, so it
 * grows and shrinks with layout. Because JavaFX canvases are bitmap-backed and do not repaint
 * automatically on size changes, this class maintains a {@code dirty} flag that callers can use to
 * trigger redraws after resizes or state changes.</p>
 *
 * <p>Usage pattern:</p>
 * <ul>
 *   <li>Subclass or hold an instance and call a custom render method when {@code dirty} is
 *   {@code true}.</li>
 *   <li>Call {@link #markDirty()} whenever the content needs to be redrawn (e.g., model update).
 *   </li>
 *   <li>Size changes automatically mark the canvas dirty.</li>
 * </ul>
 *
 * <p>Image smoothing is disabled by default via {@link GraphicsContext#setImageSmoothing(boolean)}
 * to preserve crisp pixel art (useful for grid-based games). Enable it if your content benefits
 * from interpolation.</p>
 *
 * <p>Thread-safety: JavaFX scene graph rules apply—interact with this class on the FX application
 * thread.</p>
 *
 * @since 1.0
 */
public class ResizableCanvas extends Region {

  /**
   * The underlying canvas whose size is bound to this region.
   */
  protected final Canvas canvas = new Canvas();

  /**
   * The graphics context for drawing onto {@link #canvas}.
   */
  protected final GraphicsContext gc = canvas.getGraphicsContext2D();

  /**
   * Dirty flag indicating that the canvas should be redrawn.
   *
   * <p>Set to {@code true} automatically on size changes and when {@link #markDirty()} is
   * called.</p>
   */
  protected boolean dirty = true;

  /**
   * Constructs a resizable canvas region and binds the internal canvas size to this region.
   *
   * <p>Also disables image smoothing on the graphics context and installs listeners that mark the
   * canvas dirty whenever the width or height changes.</p>
   */
  public ResizableCanvas() {
    getChildren().add(canvas);
    canvas.widthProperty().bind(widthProperty());
    canvas.heightProperty().bind(heightProperty());
    widthProperty().addListener((obs, oldVal, newVal) -> markDirty());
    heightProperty().addListener((obs, oldVal, newVal) -> markDirty());
    gc.setImageSmoothing(false);
  }

  /**
   * Marks the canvas as needing a redraw.
   *
   * <p>Typical usage is to call this after model/state changes, then perform rendering on the next
   * animation frame if {@code dirty} is {@code true}.</p>
   */
  public void markDirty() {
    dirty = true;
  }
}
