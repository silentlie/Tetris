package com.example.tetris.service.config;

import static com.example.tetris.service.config.JsonConfigurationService.CONFIG_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.tetris.core.config.Configuration;
import com.example.tetris.util.JsonUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("JsonConfigurationService")
class JsonConfigurationServiceTest {

  private static Path cfg(Path base) {
    return base.resolve(CONFIG_FILE_NAME);
  }

  private static boolean anyBackup(Path base) throws IOException {
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(base, "config.json.bak-*")) {
      return ds.iterator().hasNext();
    }
  }

  private static Path firstBackup(Path base) throws IOException {
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(base, "config.json.bak-*")) {
      Iterator<Path> it = ds.iterator();
      return it.hasNext() ? it.next() : null;
    }
  }

  @ParameterizedTest(name = "Boot via {0} (true=getCache, false=load) on missing file -> defaults saved & returned")
  @ValueSource(booleans = {true, false})
  @DisplayName("Missing config.json: create defaults, persist, return non-null")
  void missing_file_creates_and_saves_defaults(boolean viaGetCache, @TempDir Path tmp)
      throws Exception {
    Path path = cfg(tmp);
    assertFalse(Files.exists(path), "Precondition: config.json must not exist");

    JsonConfigurationService svc = new JsonConfigurationService(tmp);

    Configuration got = viaGetCache ? svc.getCache() : svc.load();
    assertNotNull(got, "Service must never return null");

    assertTrue(Files.exists(path), "Default config should be persisted to disk");
    String content = Files.readString(path, StandardCharsets.UTF_8);
    assertFalse(content.isBlank(), "Persisted JSON should not be blank");
    assertFalse(anyBackup(tmp), "No backup should be created on first write");
  }

  @Test
  @DisplayName("Existing readable config: load it (no backup, no overwrite on read)")
  void existing_readable_config_loaded(@TempDir Path tmp) throws Exception {
    Path path = cfg(tmp);
    Files.createDirectories(tmp);

    JsonConfigurationService svc = new JsonConfigurationService(tmp);
    Configuration initial = svc.load();
    assertNotNull(initial);
    assertTrue(Files.exists(path));

    Configuration reread = svc.load();
    assertNotNull(reread);
    assertTrue(Files.exists(path));
    assertFalse(anyBackup(tmp), "Readable config must not be backed up");
  }

  @Test
  @DisplayName("Corrupt file: backup old config, write defaults, return non-null")
  void corrupt_file_backed_up_and_defaults_restored(@TempDir Path tmp) throws Exception {
    Path path = cfg(tmp);
    Files.createDirectories(tmp);

    JsonConfigurationService svc = new JsonConfigurationService(tmp);
    assertNotNull(svc.load());
    assertTrue(Files.exists(path));

    Files.writeString(path, "{not:json}", StandardOpenOption.TRUNCATE_EXISTING);

    Configuration got = svc.load();
    assertNotNull(got);

    assertTrue(Files.exists(path), "config.json should be re-written with defaults");

    Path bak = firstBackup(tmp);
    assertNotNull(bak, "A backup file should be created");
    assertTrue(Files.exists(bak), "Backup file should exist on disk");

    String current = Files.readString(path, StandardCharsets.UTF_8);
    assertTrue(current.trim().startsWith("{"), "Rewritten config should be valid-looking JSON");
  }

  @Test
  @DisplayName("save(): persists to disk and updates cache to the saved instance")
  void save_persists_and_updates_cache(@TempDir Path tmp) throws Exception {
    Path path = cfg(tmp);
    JsonConfigurationService svc = new JsonConfigurationService(tmp);
    JsonUtil json = JsonUtil.getInstance();

    Configuration boot = svc.getCache();
    assertNotNull(boot);
    assertTrue(Files.exists(path));

    Configuration toSave = new Configuration();
    svc.save(toSave);

    assertSame(toSave, svc.getCache(), "Cache should be replaced by the object passed to save()");

    Configuration fromDisk = json.read(path, Configuration.class);
    assertNotNull(fromDisk, "Written file should deserialize properly");
  }

  @Test
  @DisplayName("clearCache(): forces a fresh read next time; if file is corrupted externally, it backs up and recovers")
  void clear_cache_trigger_reload_and_recover(@TempDir Path tmp) throws Exception {
    Path path = cfg(tmp);
    JsonConfigurationService svc = new JsonConfigurationService(tmp);

    assertNotNull(svc.getCache());
    assertTrue(Files.exists(path));

    Files.writeString(path, "<<<broke>>>", StandardOpenOption.TRUNCATE_EXISTING);

    svc.clearCache();
    Configuration after = svc.getCache();
    assertNotNull(after, "Service should recover to defaults on next access after corruption");
    assertTrue(Files.exists(path), "Rewritten defaults should exist");

    assertTrue(anyBackup(tmp),
        "A backup file should have been created when encountering corruption");
  }

  @Test
  @DisplayName("path(): returns <base>/config.json")
  void path_returns_expected(@TempDir Path tmp) {
    JsonConfigurationService svc = new JsonConfigurationService(tmp);
    assertEquals(tmp.resolve("config.json").normalize(), svc.path().normalize());
  }
}
