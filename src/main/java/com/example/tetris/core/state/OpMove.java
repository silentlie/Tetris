package com.example.tetris.core.state;

/**
 * Immutable AI decision describing where to place the active piece and how many times to rotate
 * it.
 *
 * <p>{@code opX} is the target horizontal column for the piece’s origin/pivot (game-specific
 * origin: typically the tetromino’s {@code position().x()}).</p>
 *
 * <p>{@code opRotate} is the number of clockwise quarter-turns to apply to the active piece
 * before/while moving it horizontally. Valid values are typically {@code 0..3}: 0 = no rotation, 1
 * = 90°, 2 = 180°, 3 = 270°.</p>
 *
 * <p>This record is intended as the output of an AI that consumes a compact game snapshot
 * (for example, {@link PureGame}) and returns an optimal placement instruction.</p>
 *
 * <p><b>Contracts & assumptions (not enforced here):</b></p>
 * <ul>
 *   <li>{@code opRotate} ∈ {0,1,2,3}.</li>
 *   <li>{@code opX} is a board column index consistent with your engine’s spawn/origin rules.</li>
 *   <li>Collision/fit checks are performed by the caller when executing the move.</li>
 * </ul>
 *
 * @param opX      target horizontal column for the active piece’s origin/pivot
 * @param opRotate number of clockwise quarter-rotations to apply (0..3)
 */
public record OpMove(int opX, int opRotate) {

}
