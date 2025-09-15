package com.example.tetris.services;

import com.example.tetris.models.Configuration;
import com.example.tetris.helpers.JsonUtil;


import java.nio.file.*;
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

  private static final Logger LOG = Logger.getLogger(JsonConfigurationService.class.getName());
  private static final String CONFIG_FILE_NAME = "config.json";

  private final Path configPath;
  private final JsonUtil json = JsonUtil.getInstance();
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
      return json.read(configPath, Configuration.class);
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
    // Atomic write handled inside Jsonutil.write
    json.write(configPath, config);
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