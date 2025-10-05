package com.example.tetris.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("JsonUtil")
class JsonUtilTest {

  record Person(String name, int age) {

  }

  @Test
  @DisplayName("Singleton: default instance is non-null and pretty-printing is enabled")
  void singleton_pretty() throws JsonProcessingException {
    JsonUtil util = JsonUtil.getInstance();
    String json = util.toJson(new Person("Alice", 30));
    assertNotNull(util, "getInstance() must not return null");
    assertTrue(json.contains("\n") || json.contains(System.lineSeparator()),
        "Pretty-printing should add newlines by default");
  }

  @Test
  @DisplayName("copy(): reconfiguring the copy doesn't affect the original")
  void copy_independent() throws JsonProcessingException {
    JsonUtil original = JsonUtil.getInstance();
    JsonUtil compact = original.copy()
        .configure(m -> m.disable(SerializationFeature.INDENT_OUTPUT));
    String pretty = original.toJson(new Person("Bob", 20));
    String flat = compact.toJson(new Person("Bob", 20));

    assertTrue(pretty.contains("\n"), "Original should still pretty-print");
    assertFalse(flat.contains("\n"), "Copy with INDENT disabled should be compact");
  }

  @Test
  @DisplayName("configure(): applies consumer and returns this (fluent)")
  void configure_applies_and_fluent(@Mock Consumer<ObjectMapper> consumer)
      throws JsonProcessingException {
    JsonUtil util = JsonUtil.getInstance().copy();
    JsonUtil returned = util.configure(m -> {
      consumer.accept(m);
      m.disable(SerializationFeature.INDENT_OUTPUT);
    });

    String json = util.toJson(new Person("Cara", 22));
    verify(consumer, times(1)).accept(any());
    assertSame(util, returned, "configure should return the same instance");
    assertFalse(json.contains("\n"), "Configuration change should take effect");
  }

  @Test
  @DisplayName("toJson()/fromJson(): round-trip of a simple record")
  void round_trip_record() throws JsonProcessingException {
    JsonUtil util = JsonUtil.getInstance();
    Person p = new Person("Dan", 40);
    String json = util.toJson(p);
    Person back = util.fromJson(json, Person.class);
    assertEquals(p, back, "Round-trip record should preserve equality");
  }

  @Test
  @DisplayName("fromJson(TypeReference): round-trip List<Person> and Map<String,Person>")
  void round_trip_generics() throws JsonProcessingException {
    JsonUtil util = JsonUtil.getInstance();

    List<Person> people = List.of(new Person("Eve", 18), new Person("Finn", 19));
    String listJson = util.toJson(people);
    List<Person> listBack = util.fromJson(listJson, new TypeReference<>() {
    });
    assertEquals(people, listBack, "List<Person> should round-trip");

    Map<String, Person> map = new LinkedHashMap<>();
    map.put("lead", new Person("Gus", 21));
    map.put("alt", new Person("Hal", 22));
    String mapJson = util.toJson(map);
    Map<String, Person> mapBack = util.fromJson(mapJson, new TypeReference<>() {
    });
    assertEquals(map, mapBack, "Map<String,Person> should round-trip");
  }

  @Test
  @DisplayName("fromJson(TypeReference): nested generics Map<String, List<Person>>")
  void round_trip_nested_generics() throws JsonProcessingException {
    JsonUtil util = JsonUtil.getInstance();
    Map<String, List<Person>> nested = Map.of(
        "teamA", List.of(new Person("Ivy", 25)),
        "teamB", List.of(new Person("Jay", 26), new Person("Kai", 27))
    );
    String json = util.toJson(nested);
    Map<String, List<Person>> back = util.fromJson(json, new TypeReference<>() {
    });
    assertEquals(nested, back, "Nested generic structure should round-trip");
  }

  @ParameterizedTest(name = "Malformed JSON should throw (case #{index})")
  @ValueSource(strings = {"", "not json", "{", "{\"name\":}", "[1,2,}", "{\"age\":\"NaN\""})
  @DisplayName("fromJson(): malformed JSON throws JsonProcessingException")
  void fromJson_malformed_throws(String bad) {
    JsonUtil util = JsonUtil.getInstance();
    assertThrows(JsonProcessingException.class, () -> util.fromJson(bad, Person.class));
  }

  @Test
  @DisplayName("toJson(null): serializes as literal \"null\"")
  void toJson_null() throws JsonProcessingException {
    JsonUtil util = JsonUtil.getInstance();
    String json = util.toJson(null);
    assertEquals("null", json, "Jackson serializes null as the literal \"null\"");
  }

  @Nested
  @DisplayName("File I/O")
  class FileIoTests {

    @TempDir
    Path tmp;

    @Test
    @DisplayName("write()/read(): round-trip POJO and create missing parent directories")
    void write_read_round_trip_creates_dirs() throws IOException {
      JsonUtil util = JsonUtil.getInstance();
      Person p = new Person("Lia", 31);
      Path nested = tmp.resolve(Path.of("a/b/c/person.json"));

      util.write(nested, p);
      assertTrue(Files.exists(nested), "File should exist after write");

      Person back = util.read(nested, Person.class);
      assertEquals(p, back, "File round-trip should preserve POJO equality");
    }

    @Test
    @DisplayName("write()/read(TypeReference): round-trip with UTF-8 and non-ASCII content")
    void write_read_utf8_generic() throws IOException {
      JsonUtil util = JsonUtil.getInstance();
      Map<String, List<Person>> payload = Map.of(
          "vn", List.of(new Person("Linh Tuấn Nguyễn", 25)),
          "jp", List.of(new Person("さくら", 23))
      );
      Path p = tmp.resolve("nested.json");
      util.write(p, payload);

      Map<String, List<Person>> back = util.read(p, new TypeReference<>() {
      });
      assertEquals(payload, back, "UTF-8 content should round-trip exactly");
    }

    @Test
    @DisplayName("write(): subsequent writes replace prior content (atomic-ish behavior)")
    void write_replaces_existing() throws IOException {
      JsonUtil util = JsonUtil.getInstance();
      Path p = tmp.resolve("value.json");

      util.write(p, new Person("First", 1));
      Person first = util.read(p, Person.class);
      assertEquals(new Person("First", 1), first);

      util.write(p, new Person("Second", 2));
      Person second = util.read(p, Person.class);
      assertEquals(new Person("Second", 2), second, "Second write should replace content");
    }
  }
}
