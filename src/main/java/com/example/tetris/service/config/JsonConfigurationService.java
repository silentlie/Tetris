package com.example.tetris.service.config;

import com.example.tetris.core.config.Configuration;
import com.example.tetris.util.JsonUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JSON-backed implementation of {@link ConfigurationService}.
 *
 * <p><strong>Behavior:</strong>
 * <ul>
 *   <li>Loads configuration from {@code config.json} in the provided base directory.</li>
 *   <li>If the file is missing, creates defaults, saves them, and returns the defaults.</li>
 *   <li>If the file exists but cannot be read, backs it up to {@code config.json.bak-<timestamp>},
 *       then writes and returns defaults.</li>
 *   <li>Writes use {@link JsonUtil#write(Path, Object)} which performs a temp-file + atomic move.
 *   </li>
 * </ul>
 *
 * <p><strong>Errors:</strong> I/O and parsing errors are caught and logged; no exceptions are
 * thrown to callers via the public API on {@link ConfigurationService}.</p>
 */
public final class JsonConfigurationService extends ConfigurationService {

  private static final Logger LOG = Logger.getLogger(JsonConfigurationService.class.getName());
  private static final String CONFIG_FILE_NAME = "config.json";
  private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  private final Path configPath;
  private final JsonUtil json = JsonUtil.getInstance();

  /**
   * Constructs a service rooted at the current working directory.
   *
   * <p>The configuration file path will be {@code <cwd>/config.json}.</p>
   */
  public JsonConfigurationService() {
    this(Path.of("").toAbsolutePath());
  }

  /**
   * Constructs a service rooted at the given base directory.
   *
   * @param baseDir base directory that will contain {@code config.json}
   */
  public JsonConfigurationService(Path baseDir) {
    this.configPath = baseDir.resolve(CONFIG_FILE_NAME);
  }

  /**
   * Loads configuration from disk.
   *
   * <p>Missing file → defaults are created, saved, and returned.<br>
   * Unreadable/corrupt file → the file is backed up, defaults are created, saved, and
   * returned.</p>
   *
   * <p>All errors are logged; never returns {@code null} (the abstract base substitutes
   * defaults).</p>
   */
  @Override
  protected Configuration loadInternal() {
    try {
      if (Files.notExists(configPath)) {
        LOG.info(() -> "Configuration not found, creating defaults at " + configPath);
        return createAndSaveDefault();
      }
      // Try read existing
      return json.read(configPath, Configuration.class);
    } catch (IOException e) {
      // Unreadable/corrupt → back it up first, then reset to defaults
      LOG.log(Level.WARNING, "Failed to read config; backing up and using defaults: " + configPath,
          e);
      backupCorruptConfig();
      return createAndSaveDefault();
    }
  }

  /**
   * Persists the given configuration to {@code config.json}.
   *
   * <p>Uses an atomic-ish write (temp file + move) inside {@link JsonUtil#write(Path, Object)}.
   * Errors are logged and not propagated.</p>
   *
   * @param configuration configuration to persist
   */
  @Override
  protected void saveInternal(Configuration configuration) {
    try {
      json.write(configPath, configuration); // atomic-ish write inside JsonUtil
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Failed to save configuration to " + configPath, e);
    }
  }

  /**
   * Returns the fully-resolved path to the configuration file.
   *
   * @return path to {@code config.json}
   */
  public Path path() {
    return configPath;
  }

  /**
   * Creates a default {@link Configuration}, attempts to persist it, and returns it.
   *
   * <p>Any write error is logged inside {@link #saveInternal(Configuration)}; the default is
   * still returned so the application can proceed with sane values.</p>
   *
   * @return a new default configuration
   */
  private Configuration createAndSaveDefault() {
    Configuration defaults = new Configuration();
    saveInternal(defaults);
    return defaults;
  }

  /**
   * Best-effort backup of an unreadable/corrupt {@code config.json}.
   *
   * <p>Renames the existing file to {@code config.json.bak-<yyyyMMdd-HHmmss>} in the same
   * directory. If the backup fails, the error is logged and load continues with defaults.</p>
   */
  private void backupCorruptConfig() {
    try {
      if (!Files.exists(configPath)) {
        return;
      }
      Path parent = configPath.getParent() == null ? Path.of(".") : configPath.getParent();
      String backupName = CONFIG_FILE_NAME + ".bak-" + TS.format(LocalDateTime.now());
      Path backup = parent.resolve(backupName);
      Files.move(configPath, backup, StandardCopyOption.REPLACE_EXISTING);
      LOG.info(() -> "Backed up corrupt config to: " + backup);
    } catch (IOException backupErr) {
      LOG.log(Level.WARNING, "Failed to backup corrupt config: " + configPath, backupErr);
    }
  }
}
