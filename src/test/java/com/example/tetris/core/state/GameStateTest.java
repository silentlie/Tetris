package com.example.tetris.core.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.tetris.core.board.Board;
import com.example.tetris.core.piece.TetrominoType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameState")
class GameStateTest {

  static PieceQueue queueOf(TetrominoType... seq) {
    Deque<TetrominoType> dq = new ArrayDeque<>(List.of(seq));
    Supplier<TetrominoType> sup = () -> dq.isEmpty() ? seq[seq.length - 1] : dq.removeFirst();
    return new PieceQueue(seq.length, sup);
  }

  @Test
  @DisplayName("spawns first piece and is not game over on an empty board")
  void spawn_on_empty() {
    var board = new Board(10, 20);
    var queue = queueOf(TetrominoType.T, TetrominoType.I, TetrominoType.O);
    var gs = new GameState(board, queue);

    assertFalse(gs.isGameOver());
    assertEquals(TetrominoType.T, gs.getTetromino().type());
    assertEquals(
        List.of(TetrominoType.I, TetrominoType.O, TetrominoType.O),
        gs.getQueue()
    );
  }

  @Test
  @DisplayName("immediate game over when spawn cannot fit (width=1)")
  void spawn_blocked() {
    var board = new Board(1, 20);
    var queue = queueOf(TetrominoType.I, TetrominoType.I, TetrominoType.I);
    var listener = mock(GameState.Listener.class);

    var gs = new GameState(board, queue);
    gs.setListener(listener);

    assertTrue(gs.isGameOver());
    verify(listener, times(1)).onGameOver();
  }

  @Test
  @DisplayName("soft drop locks when blocked; hard drop falls until blocked")
  void drops() {
    var board = new Board(10, 6);
    var queue = queueOf(TetrominoType.O, TetrominoType.O, TetrominoType.O, TetrominoType.I);
    var gs = new GameState(board, queue);

    assertTrue(gs.apply(GameIntent.HARD_DROP));
    assertFalse(gs.isGameOver());

    gs.apply(GameIntent.MOVE_RIGHT);
    gs.apply(GameIntent.HARD_DROP);
    assertFalse(gs.isGameOver());

    gs.apply(GameIntent.MOVE_RIGHT);
    while (!gs.isGameOver()) {
      if (!gs.apply(GameIntent.SOFT_DROP)) {
        break;
      }
      if (gs.getTetromino().type().equals(TetrominoType.I)) {
        break;
      }
    }

    assertFalse(gs.isGameOver());
  }

  @Test
  @DisplayName("line clear updates streak and score by rule: sum_{i=1..cleared} (streak * 100)")
  void scoring() {
    var board = new Board(4, 4);
    var queue = queueOf(TetrominoType.I, TetrominoType.I, TetrominoType.I, TetrominoType.I);
    var listener = mock(GameState.Listener.class);

    var gs = new GameState(board, queue);
    gs.setListener(listener);

    gs.apply(GameIntent.HARD_DROP);

    ArgumentCaptor<Integer> cleared = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> score = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> streak = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> best = ArgumentCaptor.forClass(Integer.class);

    verify(listener, times(1))
        .onClearLines(cleared.capture(), score.capture(), streak.capture(), best.capture());

    assertEquals(1, cleared.getValue(), "cleared exactly one line");
    assertEquals(1, streak.getValue());
    assertEquals(100, score.getValue());
    assertEquals(1, best.getValue());

    gs.apply(GameIntent.HARD_DROP);

    verify(listener, times(2)).onClearLines(cleared.capture(), score.capture(), streak.capture(),
        best.capture());

    assertEquals(1, cleared.getValue(), "cleared exactly one line");
    assertEquals(2, streak.getValue());
    assertEquals(300, score.getValue());
    assertEquals(2, best.getValue());
  }

  @Test
  @DisplayName("toPureGame emits raster + active piece + next head")
  void to_pure_game() {
    var board = new Board(10, 20);
    var queue = queueOf(TetrominoType.T, TetrominoType.Z);
    var gs = new GameState(board, queue);

    var pg = gs.toPureGame();
    assertEquals(10, pg.width());
    assertEquals(20, pg.height());
    assertEquals(TetrominoType.T, pg.tetromino().type());
    assertEquals(TetrominoType.Z, pg.next());

    int sum = 0;
    for (int y = 0; y < pg.height(); y++) {
      for (int x = 0; x < pg.width(); x++) {
        sum += pg.cells()[y][x];
      }
    }
    assertEquals(0, sum);
  }
}
