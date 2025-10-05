package com.example.tetris.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.tetris.core.state.PlayerType;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Configuration")
class ConfigurationTest {

  private static Configuration cfg(
      int w, int h, int lvl, boolean music, boolean sound, boolean extend,
      PlayerType p1, PlayerType p2
  ) {
    return new Configuration(w, h, lvl, music, sound, extend, p1, p2);
  }

  private static Stream<int[]> validEdgeTriples() {
    return Stream.of(
        new int[]{Configuration.MIN_WIDTH, Configuration.MIN_HEIGHT, Configuration.MIN_LEVEL},
        new int[]{Configuration.MIN_WIDTH, Configuration.MIN_HEIGHT, Configuration.MAX_LEVEL},
        new int[]{Configuration.MIN_WIDTH, Configuration.MAX_HEIGHT, Configuration.MIN_LEVEL},
        new int[]{Configuration.MIN_WIDTH, Configuration.MAX_HEIGHT, Configuration.MAX_LEVEL},
        new int[]{Configuration.MAX_WIDTH, Configuration.MIN_HEIGHT, Configuration.MIN_LEVEL},
        new int[]{Configuration.MAX_WIDTH, Configuration.MIN_HEIGHT, Configuration.MAX_LEVEL},
        new int[]{Configuration.MAX_WIDTH, Configuration.MAX_HEIGHT, Configuration.MIN_LEVEL},
        new int[]{Configuration.MAX_WIDTH, Configuration.MAX_HEIGHT, Configuration.MAX_LEVEL}
    );
  }

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @ParameterizedTest(name = "width out of range: {0}")
    @DisplayName("rejects invalid width")
    @ValueSource(ints = {Configuration.MIN_WIDTH - 1, Configuration.MAX_WIDTH + 1, -5, 0})
    void width(int w) {
      int h = Configuration.DEFAULT_HEIGHT;
      int lvl = Configuration.DEFAULT_LEVEL;
      assertThrows(IllegalArgumentException.class,
          () -> cfg(w, h, lvl, true, true, false, PlayerType.HUMAN, PlayerType.HUMAN));
    }

    @ParameterizedTest(name = "height out of range: {0}")
    @DisplayName("rejects invalid height")
    @ValueSource(ints = {Configuration.MIN_HEIGHT - 1, Configuration.MAX_HEIGHT + 1, -10, 0})
    void height(int h) {
      int w = Configuration.DEFAULT_WIDTH;
      int lvl = Configuration.DEFAULT_LEVEL;
      assertThrows(IllegalArgumentException.class,
          () -> cfg(w, h, lvl, true, true, false, PlayerType.HUMAN, PlayerType.HUMAN));
    }

    @ParameterizedTest(name = "level out of range: {0}")
    @DisplayName("rejects invalid level")
    @ValueSource(ints = {Configuration.MIN_LEVEL - 1, Configuration.MAX_LEVEL + 1, -1, 0, 99})
    void level(int lvl) {
      assertThrows(IllegalArgumentException.class,
          () -> cfg(Configuration.DEFAULT_WIDTH, Configuration.DEFAULT_HEIGHT, lvl,
              true, true, false, PlayerType.HUMAN, PlayerType.HUMAN));
    }

    @Test
    @DisplayName("rejects null players")
    void nulls() {
      assertThrows(NullPointerException.class,
          () -> cfg(10, 20, 1, true, true, false, null, PlayerType.HUMAN));
      assertThrows(NullPointerException.class,
          () -> cfg(10, 20, 1, true, true, false, PlayerType.HUMAN, null));
    }

    @ParameterizedTest(name = "accepts edge (w={0}, h={1}, lvl={2})")
    @DisplayName("accepts all boundary combinations")
    @MethodSource("com.example.tetris.core.config.ConfigurationTest#validEdgeTriples")
    void edges(int[] triple) {
      Configuration c = cfg(
          triple[0], triple[1], triple[2],
          true, false, true,
          PlayerType.HUMAN, PlayerType.AI
      );
      assertEquals(triple[0], c.width());
      assertEquals(triple[1], c.height());
      assertEquals(triple[2], c.level());
    }
  }

  @Nested
  @DisplayName("constructor (no-arg defaults)")
  class Defaults {

    @Test
    @DisplayName("produces validated default configuration")
    void defaults() {
      Configuration c = new Configuration();
      assertEquals(Configuration.DEFAULT_WIDTH, c.width());
      assertEquals(Configuration.DEFAULT_HEIGHT, c.height());
      assertEquals(Configuration.DEFAULT_LEVEL, c.level());
      assertEquals(Configuration.DEFAULT_MUSIC, c.music());
      assertEquals(Configuration.DEFAULT_SOUND, c.sound());
      assertEquals(Configuration.DEFAULT_EXTEND_MODE, c.extendMode());
      assertEquals(Configuration.DEFAULT_PLAYER1, c.player1());
      assertEquals(Configuration.DEFAULT_PLAYER2, c.player2());
    }

    @Test
    @DisplayName("defaults are within declared bounds")
    void bounds() {
      Configuration c = new Configuration();
      assertTrue(c.width() >= Configuration.MIN_WIDTH && c.width() <= Configuration.MAX_WIDTH);
      assertTrue(c.height() >= Configuration.MIN_HEIGHT && c.height() <= Configuration.MAX_HEIGHT);
      assertTrue(c.level() >= Configuration.MIN_LEVEL && c.level() <= Configuration.MAX_LEVEL);
    }
  }

  @Nested
  @DisplayName("withMusic(boolean)")
  class WithMusic {

    @ParameterizedTest(name = "music={0}")
    @DisplayName("toggles only the music flag; other fields unchanged")
    @ValueSource(booleans = {true, false})
    void toggles(boolean musicFlag) {
      Configuration base = cfg(10, 22, 3, !musicFlag, true, false, PlayerType.HUMAN, PlayerType.AI);
      Configuration mutated = base.withMusic(musicFlag);

      assertEquals(musicFlag, mutated.music());
      assertEquals(base.width(), mutated.width());
      assertEquals(base.height(), mutated.height());
      assertEquals(base.level(), mutated.level());
      assertEquals(base.sound(), mutated.sound());
      assertEquals(base.extendMode(), mutated.extendMode());
      assertEquals(base.player1(), mutated.player1());
      assertEquals(base.player2(), mutated.player2());

      if (musicFlag != base.music()) {
        assertNotSame(base, mutated);
        assertNotEquals(base, mutated);
      } else {
        assertNotNull(mutated);
      }
    }
  }

  @Nested
  @DisplayName("withSound(boolean)")
  class WithSound {

    @ParameterizedTest(name = "sound={0}")
    @DisplayName("toggles only the sound flag; other fields unchanged")
    @ValueSource(booleans = {true, false})
    void toggles(boolean soundFlag) {
      Configuration base = cfg(12, 24, 5, true, !soundFlag, true, PlayerType.AI, PlayerType.HUMAN);
      Configuration mutated = base.withSound(soundFlag);

      assertEquals(soundFlag, mutated.sound());
      assertEquals(base.width(), mutated.width());
      assertEquals(base.height(), mutated.height());
      assertEquals(base.level(), mutated.level());
      assertEquals(base.music(), mutated.music());
      assertEquals(base.extendMode(), mutated.extendMode());
      assertEquals(base.player1(), mutated.player1());
      assertEquals(base.player2(), mutated.player2());

      if (soundFlag != base.sound()) {
        assertNotSame(base, mutated);
        assertNotEquals(base, mutated);
      } else {
        assertNotNull(mutated);
      }
    }
  }

  @Nested
  @DisplayName("value semantics")
  class ValueSemantics {

    @Test
    @DisplayName("records compare by component values")
    void equals_hash() {
      Configuration a = cfg(10, 20, 1, true, true, false, PlayerType.HUMAN, PlayerType.AI);
      Configuration b = cfg(10, 20, 1, true, true, false, PlayerType.HUMAN, PlayerType.AI);
      Configuration c = cfg(10, 20, 2, true, true, false, PlayerType.HUMAN, PlayerType.AI);

      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
      assertNotEquals(a, c);
    }

    @ParameterizedTest(name = "different field makes records unequal: {0}")
    @DisplayName("changing any single field breaks equality")
    @CsvSource({
        "width, 11,20,1,true,true,false,HUMAN,AI",
        "height,10,21,1,true,true,false,HUMAN,AI",
        "level, 10,20,2,true,true,false,HUMAN,AI",
        "music, 10,20,1,false,true,false,HUMAN,AI",
        "sound, 10,20,1,true,false,false,HUMAN,AI",
        "extend,10,20,1,true,true,true,HUMAN,AI",
        "p1,    10,20,1,true,true,false,AI,AI",
        "p2,    10,20,1,true,true,false,HUMAN,HUMAN"
    })
    void inequality(
        String changedField,
        int w, int h, int lvl, boolean music, boolean sound, boolean extend,
        String p1, String p2
    ) {
      Configuration base = cfg(10, 20, 1, true, true, false, PlayerType.HUMAN, PlayerType.AI);
      Configuration mutated = cfg(w, h, lvl, music, sound, extend,
          PlayerType.valueOf(p1), PlayerType.valueOf(p2));
      assertNotEquals(base, mutated, "Expected inequality when changing " + changedField);
    }
  }
}
