package com.example.tetris.core.rule;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.tetris.ai.Ai;
import com.example.tetris.core.state.GameIntent;
import com.example.tetris.core.state.GameState;
import com.example.tetris.core.state.PureGame;
import com.example.tetris.ui.view.GameView;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Game")
class GameTest {

  @BeforeAll
  void initFx() {
    try {
      Platform.startup(() -> {
      });
    } catch (IllegalStateException ignored) {
      // toolkit already initialized
    }
  }

  private static Game newGame(GameState state, GameView view, GameClock clock) {
    return new Game(state, view, clock);
  }

  @Nested
  @DisplayName("constructor")
  class Ctor {

    @Test
    @DisplayName("rejects nulls")
    void rejects_nulls() {
      GameState s = mock(GameState.class);
      GameView v = mock(GameView.class);
      GameClock c = mock(GameClock.class);

      assertThrows(NullPointerException.class, () -> new Game(null, v, c));
      assertThrows(NullPointerException.class, () -> new Game(s, null, c));
      assertThrows(NullPointerException.class, () -> new Game(s, v, null));
    }
  }

  @Nested
  @DisplayName("ai(Ai)")
  class AiAttach {

    @Test
    @DisplayName("rejects null ai")
    void rejects_null_ai() {
      GameState s = mock(GameState.class);
      GameView v = mock(GameView.class);
      GameClock c = mock(GameClock.class);

      Game g = newGame(s, v, c);
      assertThrows(NullPointerException.class, () -> g.ai(null));
    }
  }

  @Nested
  @DisplayName("apply(GameIntent)")
  class ApplyIntent {

    @Test
    @DisplayName("ignored when game over")
    void ignored_when_over() {
      GameState s = mock(GameState.class);
      when(s.isGameOver()).thenReturn(true);
      GameView v = mock(GameView.class);
      GameClock c = mock(GameClock.class);

      Game g = newGame(s, v, c);
      g.apply(GameIntent.MOVE_LEFT);

      verify(s, never()).apply(any());
      verify(v, never()).markDirty();
    }

    @Test
    @DisplayName("ignored when intent is null")
    void ignored_when_null_intent() {
      GameState s = mock(GameState.class);
      when(s.isGameOver()).thenReturn(false);
      GameView v = mock(GameView.class);
      GameClock c = mock(GameClock.class);

      Game g = newGame(s, v, c);
      g.apply(null);

      verify(s, never()).apply(any());
      verify(v, never()).markDirty();
    }

    @Test
    @DisplayName("forwards to state; marks view dirty on change")
    void forwards_and_marks_dirty() {
      GameState s = mock(GameState.class);
      when(s.isGameOver()).thenReturn(false);
      when(s.apply(GameIntent.MOVE_RIGHT)).thenReturn(true);
      GameView v = mock(GameView.class);
      GameClock c = mock(GameClock.class);

      Game g = newGame(s, v, c);

      g.apply(GameIntent.MOVE_RIGHT);

      verify(s).apply(GameIntent.MOVE_RIGHT);
      verify(v).markDirty();
    }

    @Test
    @DisplayName("no dirty when state rejects")
    void no_dirty_when_rejected() {
      GameState s = mock(GameState.class);
      when(s.isGameOver()).thenReturn(false);
      when(s.apply(GameIntent.ROTATE_CW)).thenReturn(false);
      GameView v = mock(GameView.class);
      GameClock c = mock(GameClock.class);

      Game g = newGame(s, v, c);

      g.apply(GameIntent.ROTATE_CW);

      verify(v, never()).markDirty();
    }
  }

  @Nested
  @DisplayName("tick(long)")
  class Tick {

    @Test
    @DisplayName("advances clock and always renders")
    void advances_and_renders() {
      GameState s = mock(GameState.class);
      when(s.isGameOver()).thenReturn(false);
      GameView v = mock(GameView.class);
      GameClock c = mock(GameClock.class);
      when(c.fallIntervalNanos()).thenReturn(Long.MAX_VALUE);

      Game g = newGame(s, v, c);
      g.tick(16_000_000L);

      verify(c).tick(16_000_000L);
      verify(v).render(s, c);
    }

    @Test
    @DisplayName("clamps negative dt to zero")
    void clamps_negative_dt() {
      GameState s = mock(GameState.class);
      when(s.isGameOver()).thenReturn(false);
      GameView v = mock(GameView.class);
      GameClock c = mock(GameClock.class);
      when(c.fallIntervalNanos()).thenReturn(Long.MAX_VALUE);

      Game g = newGame(s, v, c);
      g.tick(-123L);

      verify(c).tick(0L);
      verify(v).render(s, c);
    }

    @Test
    @DisplayName("applies gravity-drop while accumulator ≥ interval; marks dirty on change")
    void gravity_steps() {
      GameState s = mock(GameState.class);
      when(s.isGameOver()).thenReturn(false);
      when(s.apply(GameIntent.GRAVITY_DROP)).thenReturn(true, true, false);

      GameView v = mock(GameView.class);
      GameClock c = mock(GameClock.class);
      when(c.fallIntervalNanos()).thenReturn(100L);

      Game g = newGame(s, v, c);
      g.tick(250L);

      verify(c).tick(250L);
      verify(s, times(2)).apply(GameIntent.GRAVITY_DROP);
      verify(v, times(2)).markDirty();
      verify(v).render(s, c);
    }

    @Test
    @DisplayName("AI intent applied after gravity")
    void ai_applied() {
      GameState s = mock(GameState.class);
      when(s.isGameOver()).thenReturn(false);
      when(s.toPureGame()).thenReturn(mock(PureGame.class));

      GameView v = mock(GameView.class);
      GameClock c = mock(GameClock.class);
      when(c.fallIntervalNanos()).thenReturn(Long.MAX_VALUE);

      Ai ai = mock(Ai.class);
      when(ai.tick(any())).thenReturn(GameIntent.HARD_DROP);

      Game g = newGame(s, v, c);
      g.ai(ai);
      g.tick(16_000_000L);

      verify(ai).tick(any());
      verify(s).apply(GameIntent.HARD_DROP);
      verify(v).render(s, c);
    }

    @Test
    @DisplayName("game over short-circuits time/inputs; still renders")
    void over_short_circuit() {
      GameState s = mock(GameState.class);
      when(s.isGameOver()).thenReturn(true);

      GameView v = mock(GameView.class);
      GameClock c = mock(GameClock.class);

      Game g = newGame(s, v, c);
      g.tick(8_000_000L);

      verify(c, never()).tick(anyLong());
      verify(s, never()).apply(any());
      verify(v).render(s, c);
    }
  }
}
