package com.example.tetris.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * JSON helper backed by a shared {@link ObjectMapper}.
 *
 * <p><strong>Design:</strong> This class exposes a convenient default singleton
 * via {@link #getInstance()}, configured with {@link SerializationFeature#INDENT_OUTPUT} enabled.
 * If you need different settings <em>without</em> affecting other callers, use {@link #copy()} to
 * obtain an independent instance and configure that copy.
 *
 * <p><strong>Thread-safety:</strong>
 * Jackson's {@link ObjectMapper} is thread-safe for concurrent reads/writes, but
 * <em>reconfiguration</em> is not. Call {@link #configure(Consumer)} either during bootstrap
 * (before sharing the instance) or on an instance you control exclusively (e.g., one returned by
 * {@link #copy()}). Avoid flipping features on a widely shared singleton at runtime.</p>
 *
 * <p><strong>Pretty vs compact output:</strong>
 * Pretty printing is enabled by default on the default instance. For a one-off compact output,
 * prefer using a scoped copy:
 *
 * <pre>{@code
 * JsonUtil pretty = JsonUtil.getInstance();
 * JsonUtil compact = pretty.copy()
 *     .configure(m -> m.disable(SerializationFeature.INDENT_OUTPUT));
 * compact.write(path, value); // compact file
 * }</pre>
 *
 * <p>(Alternatively, obtain an {@code ObjectWriter} with/without a pretty printer and write
 * through it; this class keeps the API simple.)</p>
 *
 * <p><strong>TypeReference quick guide (for Lists/Maps):</strong>
 * Java's type erasure hides generic parameters at runtime, so Jackson needs extra help to
 * deserialize collections. Use {@link TypeReference} to preserve the full generic type.</p>
 *
 * <pre>{@code
 * // List<T>
 * List<Configuration> list = JsonUtil.getInstance().fromJson(
 *     json, new TypeReference<List<Configuration>>() {}
 * );
 *
 * // Map<K,V>
 * Map<String, Configuration> map = JsonUtil.getInstance().fromJson(
 *     json, new TypeReference<Map<String, Configuration>>() {}
 * );
 *
 * // Nested generics (e.g., Map<String, List<Configuration>>)
 * Map<String, List<Configuration>> nested = JsonUtil.getInstance().fromJson(
 *     json, new TypeReference<Map<String, List<Configuration>>>() {}
 * );
 *
 * // (Optional) reuse a constant to avoid reallocating:
 * private static final TypeReference<List<Configuration>> LIST_OF_CFG =
 *     new TypeReference<>() {};
 * List<Configuration> again = JsonUtil.getInstance().fromJson(json, LIST_OF_CFG);
 * }</pre>
 */
public final class JsonUtil {

  /**
   * Default, shared instance (pretty-printing enabled).
   */
  private static final JsonUtil DEFAULT = new JsonUtil();

  /**
   * The underlying {@link ObjectMapper}. Thread-safe for use, not for live reconfiguration.
   */
  private final ObjectMapper mapper;

  /**
   * Hidden constructor for the default singleton. Registers common features/modules here.
   */
  private JsonUtil() {
    this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    // Add modules here if your project needs them, e.g. JavaTimeModule.
    // mapper.registerModule(new JavaTimeModule());
  }

  /**
   * Internal constructor used by {@link #copy()}.
   */
  private JsonUtil(ObjectMapper mapper) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  /**
   * Access the default singleton.
   */
  public static JsonUtil getInstance() {
    return DEFAULT;
  }

  /**
   * Creates a temporary sibling file for safe write-then-rename cycles.
   *
   * <p>The temp file is created in the same directory as {@code target}
   * so that a subsequent {@link Files#move(Path, Path, CopyOption...) move} can remain on the same
   * filesystem.</p>
   */
  private static Path tempSibling(Path target) throws IOException {
    Path parent = target.getParent();
    if (parent == null) {
      parent = Path.of(".");
    }
    String prefix = target.getFileName().toString() + ".tmp-";
    return Files.createTempFile(parent, prefix, ".json");
  }

  /**
   * Returns a new {@code JsonUtil} whose mapper is a deep copy of this instance's mapper.
   *
   * <p>Implemented via {@link ObjectMapper#copy()}, which clones configuration,
   * features, modules, and mix-ins. Reconfiguring the returned instance will not affect this
   * instance, and vice versa.</p>
   */
  public JsonUtil copy() {
    return new JsonUtil(this.mapper.copy());
  }

  /**
   * Optionally mutate this instance's mapper configuration.
   *
   * <p><strong>Important:</strong> Changing features (e.g., toggling pretty printing)
   * affects <em>all</em> concurrent users of this instance. Prefer calling this during startup or
   * use {@link #copy()} to obtain an isolated instance and configure that.</p>
   *
   * @param configurer a function that mutates the underlying mapper
   *
   * @return {@code this} for chaining
   */
  public synchronized JsonUtil configure(Consumer<ObjectMapper> configurer) {
    configurer.accept(this.mapper);
    return this;
  }

  /**
   * Serializes an object to a JSON string.
   *
   * <p>Jackson serializes {@code null} as the literal string {@code "null"}.</p>
   *
   * @param value object to serialize (maybe {@code null})
   *
   * @return JSON string
   *
   * @throws JsonProcessingException if serialization fails
   */
  public String toJson(Object value) throws JsonProcessingException {
    return mapper.writeValueAsString(value);
  }

  /**
   * Deserializes a JSON string to a concrete, non-generic type.
   *
   * <p>Use this for simple targets like {@code Player.class} or {@code Configuration.class}.</p>
   *
   * @param json JSON content (non-{@code null})
   * @param type target class (non-{@code null})
   * @param <T>  result type
   *
   * @return deserialized instance
   *
   * @throws JsonProcessingException if parsing fails
   */
  public <T> T fromJson(String json, Class<T> type) throws JsonProcessingException {
    Objects.requireNonNull(json, "json");
    Objects.requireNonNull(type, "type");
    return mapper.readValue(json, type);
  }

  /**
   * Deserializes a JSON string to a <em>generic</em> type using {@link TypeReference}.
   *
   * <p><strong>When to use:</strong> any time your target is a collection or has generics
   * (e.g., {@code List<Configuration>}, {@code Map<String, Configuration>},
   * {@code Map<String, List<Configuration>>}).</p>
   *
   * <pre>{@code
   * // List<T>
   * List<Configuration> cfgs = JsonUtil.getInstance().fromJson(
   *     json, new TypeReference<List<Configuration>>() {}
   * );
   *
   * // Map<K,V>
   * Map<String, Configuration> byName = JsonUtil.getInstance().fromJson(
   *     json, new TypeReference<Map<String, Configuration>>() {}
   * );
   *
   * // Nested generics
   * Map<String, List<Configuration>> grouped = JsonUtil.getInstance().fromJson(
   *     json, new TypeReference<Map<String, List<Configuration>>>() {}
   * );
   * }</pre>
   *
   * @param json    JSON content
   * @param typeRef captures the full generic type information
   * @param <T>     result type
   *
   * @return deserialized instance
   *
   * @throws JsonProcessingException if parsing fails
   */
  public <T> T fromJson(String json, TypeReference<T> typeRef) throws JsonProcessingException {
    return mapper.readValue(json, typeRef);
  }

  /**
   * Writes an object as JSON to a file using UTF-8 with an "atomic-ish" strategy: write to a temp
   * sibling and then {@link Files#move(Path, Path, CopyOption...) move} it into place.
   *
   * <p><strong>Note:</strong> {@link StandardCopyOption#ATOMIC_MOVE} may not be supported
   * on all filesystems. If unsupported, the move may fall back to a non-atomic replace or throw
   * {@link java.nio.file.AtomicMoveNotSupportedException} depending on the platform.</p>
   *
   * @param path  destination path (parent directories will be created)
   * @param value object to serialize
   *
   * @throws IOException if writing or moving fails
   */
  public void write(Path path, Object value) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Path tmp = tempSibling(path);
    try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
      mapper.writeValue(w, value);
    }
    Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  /**
   * Reads JSON from a file path (UTF-8) into a concrete, non-generic type.
   *
   * @param path source path
   * @param type target class
   * @param <T>  result type
   *
   * @return deserialized instance
   *
   * @throws IOException if reading or parsing fails
   */
  public <T> T read(Path path, Class<T> type) throws IOException {
    try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return mapper.readValue(r, type);
    }
  }

  /**
   * Reads JSON from a file path (UTF-8) into a <em>generic</em> type captured by
   * {@link TypeReference}.
   *
   * <pre>{@code
   * // List<T>
   * List<Configuration> list = JsonUtil.getInstance().read(
   *     path, new TypeReference<List<Configuration>>() {}
   * );
   *
   * // Map<K,V>
   * Map<String, Configuration> map = JsonUtil.getInstance().read(
   *     path, new TypeReference<Map<String, Configuration>>() {}
   * );
   *
   * // Deeply nested
   * Map<String, List<Configuration>> nested = JsonUtil.getInstance().read(
   *     path, new TypeReference<Map<String, List<Configuration>>>() {}
   * );
   * }</pre>
   *
   * @param path    source path
   * @param typeRef target type reference
   * @param <T>     result type
   *
   * @return deserialized instance
   *
   * @throws IOException if reading or parsing fails
   */
  public <T> T read(Path path, TypeReference<T> typeRef) throws IOException {
    try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return mapper.readValue(r, typeRef);
    }
  }
}
