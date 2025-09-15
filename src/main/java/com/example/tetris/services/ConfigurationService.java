package com.example.tetris.services;

import com.example.tetris.models.Configuration;

/**
 * Abstract base for configuration persistence with a simple in-memory cache.
 *
 * <p><strong>Contract:</strong>
 * <ul>
 *   <li>{@link #getCache()} lazily loads from the backing store on first call and then returns the
 *   cached instance.</li>
 *   <li>{@link #load()} forces a reload from the backing store and refreshes the cache.</li>
 *   <li>{@link #save(Configuration)} persists the given configuration and updates the cache.</li>
 *   <li>{@link #clearCache()} clears only the in-memory cache; it does not modify the backing
 *   store.</li>
 *   <li>All public methods <em>never return {@code null}</em>. If {@link #loadInternal()} returns
 *   {@code null}, a new default {@link Configuration} is used instead.</li>
 * </ul>
 *
 * <p><strong>Threading:</strong> This class is not synchronized. If accessed from multiple threads,
 * add synchronization at the call site or in a subclass.</p>
 *
 * <p><strong>Subclass duties:</strong> Implement {@link #loadInternal()} and
 * {@link #saveInternal(Configuration)} to perform actual I/O (e.g., JSON file, database, remote
 * API). These hooks may return {@code null} or throw; callers will still receive a non-null
 * {@link Configuration} via the public API.</p>
 */
public abstract class ConfigurationService {

  /**
   * In-memory snapshot of the last loaded or saved configuration.
   */
  private Configuration cache;

  /**
   * Returns the cached configuration, loading it from the backing store if needed.
   *
   * <p>First call performs a lazy load via {@link #loadInternal()} and caches the result.
   * Subsequent calls return the cached instance until {@link #load()} or {@link #clearCache()} is
   * invoked.</p>
   *
   * @return a non-null configuration (defaults are used if loading fails or returns {@code null})
   */
  public final Configuration getCache() {
    if (cache == null) {
      cache = loadInternalNonNull();
    }
    return cache;
  }

  /**
   * Forces a reload from the backing store and refreshes the cache.
   *
   * <p>Use this when you explicitly want to re-read from disk/DB even if a cached value
   * exists.</p>
   *
   * @return a non-null configuration (defaults are used if loading fails or returns {@code null})
   */
  public final Configuration load() {
    cache = loadInternalNonNull();
    return cache;
  }

  /**
   * Persists the given configuration and updates the cache.
   *
   * <p>Implementations should ensure durability (e.g., atomic writes). Errors should be handled
   * within {@link #saveInternal(Configuration)} according to your policy (log, retry, etc.).</p>
   *
   * @param configuration configuration to persist (maybe the same instance as the cache)
   */
  public final void save(Configuration configuration) {
    saveInternal(configuration);
    cache = configuration;
  }

  /**
   * Clears the in-memory cache. The next call to {@link #getCache()} or {@link #load()} will load
   * from the backing store again.
   */
  public final void clearCache() {
    cache = null;
  }

  // ----- subclass hooks -----

  /**
   * Load the configuration from the backing store.
   *
   * <p>May return {@code null} to indicate failure or absence; the public API will substitute a
   * default {@link Configuration} in that case.</p>
   *
   * @return the loaded configuration, or {@code null} on failure/absence
   */
  protected abstract Configuration loadInternal();

  /**
   * Save the configuration to the backing store.
   *
   * <p>Implementations should handle I/O errors according to project policy (e.g., catch and log).
   * The public API does not throw.</p>
   *
   * @param configuration configuration to persist
   */
  protected abstract void saveInternal(Configuration configuration);

  // ----- guard to ensure non-null contract -----

  /**
   * Helper that calls {@link #loadInternal()} and guarantees a non-null result by falling back to a
   * fresh default {@link Configuration} instance.
   */
  private Configuration loadInternalNonNull() {
    Configuration c = loadInternal();
    return (c != null) ? c : new Configuration();
  }
}
