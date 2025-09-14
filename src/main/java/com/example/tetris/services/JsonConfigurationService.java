package com.example.tetris.services;

import com.example.tetris.helpers.AppContext;
import com.example.tetris.helpers.ConfigurationCache;
import com.example.tetris.models.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Json implementation of ConfigurationService
 *
 * This file will:
 *  - Load configuration from a config.json file in the root directory if none exist it will create one with default settings.
 *  - Save a configuration back to config.json safely via atomic writes.
 *  - Fall back to default configuration if file is missing or invalid.
 */

public class JsonConfigurationService implements ConfigurationService {

  private static final Logger LOG = Logger.getLogger(JsonConfigurationService.class.getName()); // Logger for warnings/info messages
  private static final String CONFIG_FILE_NAME = "config.json"; // File name of the JSON config file

  private final Path configPath;  // Path to the config file in the root directory.
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();  // Jackson ObjectMapper used for JSON serialisation/deserialisation

  /**
   * Constructor that determines the absolute path of config.json
   * in the application root directory
   */
  public JsonConfigurationService() {
    Path baseDir = Path.of("").toAbsolutePath();  // current working directory
    this.configPath = baseDir.resolve(CONFIG_FILE_NAME);
  }

  /**
   * Load configuration from config.json
   *  - If file does not exist then create defaults and save them.
   *  - If file exists but cannot be read then log warning, reset to defaults.
   *
   */

  @Override
  public Configuration load() {
    try {
      if (Files.notExists((configPath))) {
        LOG.info(() -> "config not found, creating defaults at: " + configPath);
        return createAndSaveDefault();
      }
      // Parse existing JSON into a Configuration object
      return mapper.readValue(configPath.toFile(), Configuration.class);
    } catch(Exception e) {
      // On any error, log and reset to defaults
      LOG.warning("Failed to load config.json: Using defaults. Reason: " + e.getMessage());
      return createAndSaveDefault();
    }
  }

  /**
   *
   * Saves to provided Configuration to config.json.
   * Uses a temporary file and atomic move to avoid any corruption/issues.
   */
  @Override
  public void save(Configuration config) {
    Path dir = configPath.getParent();
    if (dir == null) dir = Path.of(".");  // fallback to current directory

    try {
      // Write json to a temporary file in the same directory
      Path temp = Files.createTempFile(dir, "config", ".tmp");
      try {
        mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), config);
        try {
          // Atomic move: replace old file with temp in one step
          Files.move(temp, configPath,
                      StandardCopyOption.REPLACE_EXISTING,
                      StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException amnse) {
          // If atomic moves are not supported then fallback to regular replace
          LOG.log(Level.INFO, "Atomic move not supported; falling back to non-atomic replace.");
          Files.move(temp, configPath, StandardCopyOption.REPLACE_EXISTING);
        }
      } finally {
        // Best effort to clean-up and remove temp files if it was not moved
        try { Files.deleteIfExists(temp);} catch(IOException ignore) {}
      }
    } catch (IOException ioe) {
      // Error if saving completely fails
      throw new RuntimeException("Failed to save configuration to " + configPath, ioe);
    }
  }

  /**
   * Create a default Configuration (using record defaults),
   * Save it and return it.
   *
   */
  private Configuration createAndSaveDefault() {
    Configuration defaults = new Configuration(); // no-arg defaults
    save(defaults);
    return defaults;
  }

}