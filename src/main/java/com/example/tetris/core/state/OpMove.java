package com.example.tetris.core.state;

/**
 * Immutable AI decision describing where to place the active piece and how many times to rotate
 * it.
 *
 * <p><strong>Semantics</strong></p>
 * <ul>
 *   <li>{@code opX} is the target board column for the piece’s origin/pivot
 *       (typically the tetromino’s {@code position().x()}).</li>
 *   <li>{@code opRotate} is the number of clockwise quarter-turns to apply to the active piece
 *       before/while translating it horizontally. Valid values are {@code 0..3}:
 *       {@code 0}=no rotation, {@code 1}=90°, {@code 2}=180°, {@code 3}=270°.</li>
 * </ul>
 *
 * <p><strong>Usage</strong></p>
 *
 * <p>This record is intended as the output of an AI that consumes a compact game snapshot
 * (for example, {@link PureGame}) and returns an optimal placement instruction. The caller is
 * responsible for executing the movement/rotation and performing collision/fit checks.</p>
 *
 * <p><strong>Contracts &amp; assumptions (not enforced here unless stated)</strong></p>
 * <ul>
 *   <li>{@code opRotate} ∈ {0,1,2,3} (validated in the canonical constructor).</li>
 *   <li>{@code opX} is a valid column index for your engine’s spawn/origin rules and board width.
 *       This is <em>not</em> validated here because it depends on runtime board configuration.</li>
 *   <li>Rotation system (e.g., SRS kicks) and gravity/lock timing are engine concerns.</li>
 * </ul>
 *
 * @see PureGame
 * @since 1.0
 */
public record OpMove(
    int opX,
    int opRotate
) {

}
