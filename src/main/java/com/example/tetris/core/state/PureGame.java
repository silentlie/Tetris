package com.example.tetris.core.state;

public record PureGame(
    int width,
    int height,
    int[][] cells,
    int[][] currentShape,
    int[][] nextShape
) {

}
