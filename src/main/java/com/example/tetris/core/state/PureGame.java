package com.example.tetris.core.state;

/**
 * Immutable data-transfer object (DTO) capturing a minimal snapshot of a Tetris game state.
 *
 * <p>Intended for lightweight rendering or AI evaluation, this record flattens the game state
 * into simple 2D arrays and dimensions without exposing full object graphs like
 * {@link com.example.tetris.core.board.Board} or
 * {@link com.example.tetris.core.piece.Tetromino}.</p>
 *
 * <p>Fields:</p>
 * <ul>
 *   <li>{@code width}, {@code height} — board dimensions in columns × rows.</li>
 *   <li>{@code cells} — settled board occupancy as a {@code height × width} matrix of 0/1 values
 *       (1 = occupied, 0 = empty).</li>
 *   <li>{@code currentShape} — 0/1 matrix encoding the active tetromino’s blocks in its current
 *       rotation. Dimensions correspond to the tetromino’s bounding box.</li>
 *   <li>{@code nextShape} — 0/1 matrix encoding the next tetromino in the preview queue, always
 *       in {@link com.example.tetris.core.piece.Rotation#UP} orientation.</li>
 * </ul>
 *
 * <p>All arrays are expected to be precomputed and immutable by convention. Callers should not
 * modify them after construction.</p>
 *
 * @param width        board width (columns)
 * @param height       board height (rows)
 * @param cells        settled board as a 0/1 matrix of size {@code height × width}
 * @param currentShape active tetromino’s current shape as a 0/1 matrix
 * @param nextShape    next tetromino’s shape (up orientation) as a 0/1 matrix
 */
public record PureGame(
    int width,
    int height,
    int[][] cells,
    int[][] currentShape,
    int[][] nextShape
) {

}