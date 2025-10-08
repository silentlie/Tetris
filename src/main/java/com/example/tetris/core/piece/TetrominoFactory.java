package com.example.tetris.core.piece;

import com.example.tetris.core.board.Cell;

/**
 * Factory class responsible for creating Tetromino pieces.
 * Centralizes object creation logic and supports random piece generation.
 */
public final class TetrominoFactory {

    private TetrominoFactory() {
        // Prevent instantiation
    }

    /**
     * Creates a new Tetromino at the given spawn position.
     * If no specific type is provided, a random one is selected.
     *
     * @param spawnPosition the initial grid position of the Tetromino
     * @return a new Tetromino instance
     */
    public static Tetromino createTetromino(Cell spawnPosition) {
        TetrominoType type = TetrominoType.randomType();
        return new Tetromino(type, spawnPosition, Rotation.UP);
    }
}
