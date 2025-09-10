package com.example.tetris.services;

import com.example.tetris.models.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonConfigurationService implements ConfigurationService {

  private static final Logger LOG = Logger.getLogger(JsonConfigurationService.class.getName());
  private static final String CONFIG_FILE_NAME = "config.json";

  private final Path configPath;
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  public JsonConfigurationService() {
    Path baseDir = Path.of("").toAbsolutePath();
    this.configPath = baseDir.resolve(CONFIG_FILE_NAME);
  }

  @Override
  public Configuration load() {
    try {
      if (Files.notExists((configPath))) {
        LOG.info(() -> "config not found, creating defaults at: " + configPath);
        return createAndSaveDefault();
      }
      return mapper.readValue(configPath.toFile(), Configuration.class);
    } catch(Exception e) {
      LOG.warning("Failed to load config.json: Using defaults. Reason: " + e.getMessage());
      return createAndSaveDefault();
    }
  }

  @Override
  public void save(Configuration config) {
    Path dir = configPath.getParent();
    if (dir == null) dir = Path.of(".");

    try {
      // Write to temp file in same dir
      Path temp = Files.createTempFile(dir, "config", ".tmp");
      try {
        mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), config);
        try {
          Files.move(temp, configPath,
                      StandardCopyOption.REPLACE_EXISTING,
                      StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException amnse) {
          // Fallback if file system does not support atomic move
          LOG.log(Level.INFO, "Atomic move not supported; falling back to non-atomic replace.");
          Files.move(temp, configPath, StandardCopyOption.REPLACE_EXISTING);
        }
      } finally {
        //Best-effor cleanup if temp still exists
        try { Files.deleteIfExists(temp);} catch(IOException ignore) {}
      }
    } catch (IOException ioe) {
      throw new RuntimeException("Failed to save configuration to " + configPath, ioe);
    }
  }

  private Configuration createAndSaveDefault() {
    Configuration defaults = new Configuration(); // no-arg defaults
    save(defaults);
    return defaults;
  }

}