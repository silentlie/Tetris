package com.example.tetris.helpers;

import com.example.tetris.models.Configuration;
import com.example.tetris.services.ConfigurationService;

// In memory cache for configuration
// Avoids repeat disk reads by holding the last loaded/saved config.

public final class ConfigurationCache {
  private Configuration cached;

  // return the cached configuration if available
  // Else load it from the service and store it
  public Configuration getOrLoad(ConfigurationService svc) {
    if (cached == null) {
      cached = svc.load();

    }
    return cached;
  }

  // update the cached configuration after save
  public void set(Configuration cfg) {
    this.cached = cfg;
  }
}