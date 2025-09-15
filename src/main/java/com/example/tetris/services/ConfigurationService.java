package com.example.tetris.services;
import com.example.tetris.models.Configuration;


/*
2. Implement ConfigurationService for JSON persistence

File: src/main/java/com/example/tetris/services/ConfigurationService.java
Define an interface with methods:
Configuration load()
void save(Configuration config)
Implement a JSON-based service using Jackson
Store config.json at the application root directory (same level as the JAR or exe)
On load:
If config.json is missing or invalid → create a default configuration, save it, and return it
Otherwise → return the loaded configuration
Atomic safe writes: write to a temporary file and replace the old one in a single move to prevent corruption
Error strategy: if load fails, fall back to defaults, persist them immediately, and show a non-blocking warning (log or alert)
*/

public interface ConfigurationService {
  Configuration load();
  void save(Configuration config);
}
