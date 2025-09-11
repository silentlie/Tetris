package com.example.tetris.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Utility methods for serializing/deserializing JSON using a single, preconfigured Jackson
 * {@link ObjectMapper}.
 *
 * <p><strong>TypeReference quick guide (for Lists/Maps):</strong>
 * Java's type erasure hides generic parameters at runtime, so Jackson needs extra help to
 * deserialize collections. Use {@link TypeReference} to preserve the full generic type.
 *
 * <pre>{@code
 * // List<T>
 * List<Configuration> list =
 *     JsonUtil.fromJson(json, new TypeReference<List<Configuration>>() {});
 *
 * // Map<K,V>
 * Map<String, Configuration> map =
 *     JsonUtil.fromJson(json, new TypeReference<Map<String, Configuration>>() {});
 *
 * // Nested generics (e.g., Map<String, List<Configuration>>)
 * Map<String, List<Configuration>> nested =
 *     JsonUtil.fromJson(json, new TypeReference<Map<String, List<Configuration>>>() {});
 *
 * // (Optional) reuse a constant to avoid reallocating:
 * private static final TypeReference<List<Configuration>> LIST_OF_CFG =
 *     new TypeReference<>() {};
 * List<Configuration> again = JsonUtil.fromJson(json, LIST_OF_CFG);
 * }</pre>
 *
 * <p><strong>Thread-safety:</strong> The mapper is configured once and then used concurrently.
 * <br><strong>Pretty printing:</strong> {@link SerializationFeature#INDENT_OUTPUT} is enabled by
 * default.
 */
public class JsonUtil {

  /**
   * Shared, preconfigured {@link ObjectMapper}.
   *
   * <p>Defaults:
   * <ul>
   *   <li>{@link SerializationFeature#INDENT_OUTPUT} enabled</li>
   * </ul>
   * Add modules (e.g., Java Time, Kotlin) here if your project needs them.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper().enable(
      SerializationFeature.INDENT_OUTPUT);

  /**
   * Hidden constructor — this is a static utility.
   */
  private JsonUtil() {
  }

  /**
   * Serializes an object to a JSON string.
   *
   * @param value object to serialize (must not be {@code null})
   * @return JSON string
   * @throws UncheckedIOException if serialization fails
   */
  public static String toJson(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw wrap(e, "Serializing to JSON string");
    }
  }

  /**
   * Deserializes a JSON string to a concrete, non-generic type.
   *
   * <p>Use this for simple targets like {@code Player.class} or {@code Configuration.class}.
   *
   * @param json JSON content
   * @param type target class
   * @param <T>  result type
   * @return deserialized instance
   * @throws UncheckedIOException if parsing fails
   */
  public static <T> T fromJson(String json, Class<T> type) {
    try {
      return MAPPER.readValue(json, type);
    } catch (IOException e) {
      throw wrap(e, "Parsing JSON string -> " + type.getSimpleName());
    }
  }

  /**
   * Deserializes a JSON string to a <em>generic</em> type using {@link TypeReference}.
   *
   * <p><strong>When to use:</strong> any time your target is a collection or has generics
   * (e.g., {@code List<Configuration>}, {@code Map<String, Configuration>},
   * {@code Map<String, List<Configuration>>}).
   *
   * <p><strong>Examples:</strong>
   * <pre>{@code
   * // List<T>
   * List<Configuration> cfgs =
   *     JsonUtil.fromJson(json, new TypeReference<List<Configuration>>() {});
   *
   * // Map<K,V>
   * Map<String, Configuration> byName =
   *     JsonUtil.fromJson(json, new TypeReference<Map<String, Configuration>>() {});
   *
   * // Nested generics
   * Map<String, List<Configuration>> grouped =
   *     JsonUtil.fromJson(json, new TypeReference<Map<String, List<Configuration>>>() {});
   * }</pre>
   *
   * @param json    JSON content
   * @param typeRef captures the full generic type information
   * @param <T>     result type
   * @return deserialized instance
   * @throws UncheckedIOException if parsing fails
   */
  public static <T> T fromJson(String json, TypeReference<T> typeRef) {
    try {
      return MAPPER.readValue(json, typeRef);
    } catch (IOException e) {
      throw wrap(e, "Parsing JSON string -> " + typeRef.getType());
    }
  }

  /**
   * Writes an object as JSON to a file path using UTF-8 with an "atomic-ish" strategy: write to a
   * temp sibling and then {@link Files#move(Path, Path, CopyOption...) move} it into place.
   *
   * @param path  destination path (parent directories will be created)
   * @param value object to serialize
   * @throws UncheckedIOException if writing fails
   */
  public static void write(Path path, Object value) {
    try {
      Files.createDirectories(path.getParent());
      // Atomic-ish write: write temp then move
      Path tmp = tempSibling(path);
      try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
        MAPPER.writeValue(w, value);
      }
      Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw wrap(e, "Writing JSON to " + path);
    }
  }

  /**
   * Reads JSON from a file path (UTF-8) into a concrete, non-generic type.
   *
   * @param path source path
   * @param type target class
   * @param <T>  result type
   * @return deserialized instance
   * @throws UncheckedIOException if reading or parsing fails
   */
  public static <T> T read(Path path, Class<T> type) {
    try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return MAPPER.readValue(r, type);
    } catch (IOException e) {
      throw wrap(e, "Reading JSON from " + path + " -> " + type.getSimpleName());
    }
  }

  /**
   * Reads JSON from a file path (UTF-8) into a <em>generic</em> type captured by
   * {@link TypeReference}.
   *
   * <p><strong>Common patterns:</strong>
   * <pre>{@code
   * // List<T>
   * List<Configuration> list =
   *     JsonUtil.read(path, new TypeReference<List<Configuration>>() {});
   *
   * // Map<K,V>
   * Map<String, Configuration> map =
   *     JsonUtil.read(path, new TypeReference<Map<String, Configuration>>() {});
   *
   * // Deeply nested
   * Map<String, List<Configuration>> nested =
   *     JsonUtil.read(path, new TypeReference<Map<String, List<Configuration>>>() {});
   * }</pre>
   *
   * @param path    source path
   * @param typeRef target type reference
   * @param <T>     result type
   * @return deserialized instance
   * @throws UncheckedIOException if reading or parsing fails
   */
  public static <T> T read(Path path, TypeReference<T> typeRef) {
    try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return MAPPER.readValue(r, typeRef);
    } catch (IOException e) {
      throw wrap(e, "Reading JSON from " + path + " -> " + typeRef.getType());
    }
  }

  /**
   * Wraps checked Jackson/IO exceptions in an {@link UncheckedIOException} with a descriptive
   * action message.
   *
   * @param e      original exception
   * @param action human-readable description of the attempted action
   * @return unchecked exception suitable for rethrow
   */
  private static RuntimeException wrap(Exception e, String action) {
    return new UncheckedIOException(action + " failed",
        e instanceof IOException io ? io : new IOException(e));
  }

  /**
   * Creates a temporary file in the same directory as the target to enable a safe write-then-rename
   * cycle.
   *
   * @param target final destination path
   * @return temporary sibling path
   * @throws IOException if the temp file cannot be created
   */
  private static Path tempSibling(Path target) throws IOException {
    String prefix = target.getFileName().toString() + ".tmp-";
    return Files.createTempFile(target.getParent(), prefix, ".json");
  }
}
