package com.example.tetris.service.config;

import com.example.tetris.core.config.Configuration;

/**
 * Abstract base for configuration persistence with a simple in-memory cache.
 *
 * <p><strong>Contract:</strong>
 * <ul>
 *   <li>{@link #getCache()} lazily loads from the backing store on first call and then returns the
 *       cached instance.</li>
 *   <li>{@link #load()} forces a reload from the backing store and refreshes the cache.</li>
 *   <li>{@link #save(Configuration)} persists the given configuration and updates the cache.</li>
 *   <li>{@link #clearCache()} clears only the in-memory cache; it does not modify the backing
 *       store.</li>
 *   <li>All public methods <em>never return {@code null}</em>. If {@link #loadInternal()} returns
 *       {@code null}, a new default {@link Configuration} is used instead.</li>
 * </ul>
 *
 * <p><strong>Threading & synchronization:</strong>
 * <ul>
 *   <li>All public methods are <em>synchronized</em> on this instance. Calls to {@code getCache()},
 *       {@code load()}, {@code save(Configuration)}, and {@code clearCache()} are mutually
 *       exclusive.</li>
 *   <li>The cached value is stored in a {@code volatile} field for safe publication to other
 *       threads.</li>
 *   <li><em>Important:</em> {@link #loadInternal()} and {@link #saveInternal(Configuration)} are
 *       invoked while the service monitor is held (i.e., from synchronized methods).
 *       Implementations must not call back into any public method on this service (would deadlock),
 *       and should keep I/O reasonably fast since it blocks other callers.</li>
 * </ul>
 *
 * <p><strong>Error policy:</strong>
 * <ul>
 *   <li>If {@link #loadInternal()} returns {@code null}, the public API substitutes
 *       {@code new Configuration()}.</li>
 *   <li>If {@code loadInternal()} or {@code saveInternal(Configuration)} throws an exception, it
 *       will propagate to the caller unless the implementation catches it. If you want the public
 *       API to be non-throwing, <em>catch and log</em> inside your implementation and return
 *       {@code null} (for load) to trigger the default.</li>
 * </ul>
 *
 * <p><strong>Subclass duties:</strong> Implement {@link #loadInternal()} and
 * {@link #saveInternal(Configuration)} to perform actual I/O (e.g., JSON file, database, remote
 * API). See each method’s Javadoc for threading and reentrancy notes.</p>
 */
public abstract class ConfigurationService {

  /**
   * In-memory snapshot of the last loaded or saved configuration. Published with {@code volatile}
   * to ensure visibility across threads.
   */
  private volatile Configuration cache;

  /**
   * Returns the cached configuration, loading it from the backing store if needed.
   *
   * <p>First call performs a lazy load via {@link #loadInternal()} and caches the result.
   * Subsequent calls return the cached instance until {@link #load()} or {@link #clearCache()} is
   * invoked.</p>
   *
   * <p><strong>Threading:</strong> Synchronized. May perform I/O under the lock on a cold start.
   * </p>
   *
   * @return a non-null configuration (defaults are used if loading returns {@code null})
   */
  public final synchronized Configuration getCache() {
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
   * <p><strong>Threading:</strong> Synchronized. Performs I/O under the lock.</p>
   *
   * @return a non-null configuration (defaults are used if loading returns {@code null})
   */
  public final synchronized Configuration load() {
    cache = loadInternalNonNull();
    return cache;
  }

  /**
   * Persists the given configuration and updates the cache.
   *
   * <p>Implementations should ensure durability (e.g., atomic writes). Consider catching and
   * logging I/O errors inside {@link #saveInternal(Configuration)} if you do not want exceptions to
   * escape.</p>
   *
   * <p><strong>Threading:</strong> Synchronized. Performs I/O under the lock.</p>
   *
   * @param configuration configuration to persist (maybe the same instance as the cache)
   */
  public final synchronized void save(Configuration configuration) {
    saveInternal(configuration);
    cache = configuration;
  }

  /**
   * Clears the in-memory cache. The next call to {@link #getCache()} or {@link #load()} will load
   * from the backing store again.
   *
   * <p><strong>Threading:</strong> Synchronized.</p>
   */
  public final synchronized void clearCache() {
    cache = null;
  }

  // ----- subclass hooks -----

  /**
   * Load the configuration from the backing store.
   *
   * <p><strong>Threading:</strong> Called while holding this service's intrinsic lock (from a
   * synchronized method). Do <em>not</em> call back into any public method on this service from
   * within this method (risk of deadlock). Keep I/O responsive; long-running work will block other
   * callers.</p>
   *
   * <p><strong>Return / error policy:</strong> May return {@code null} to indicate failure or
   * absence; the public API will substitute a default {@link Configuration}. Any thrown exception
   * will propagate unless caught here.</p>
   *
   * @return the loaded configuration, or {@code null} on failure/absence
   */
  protected abstract Configuration loadInternal();

  /**
   * Save the configuration to the backing store.
   *
   * <p><strong>Threading:</strong> Called while holding this service's intrinsic lock. Do
   * <em>not</em> call back into public methods on this service from within this method (risk of
   * deadlock). Keep I/O responsive.</p>
   *
   * <p><strong>Error policy:</strong> If this method throws, the exception propagates to the
   * caller. If you prefer a non-throwing public API, catch and log here according to project
   * policy.</p>
   *
   * @param configuration configuration to persist
   */
  protected abstract void saveInternal(Configuration configuration);

  // ----- guard to ensure non-null contract -----

  /**
   * Calls {@link #loadInternal()} and guarantees a non-null result by falling back to a fresh
   * default {@link Configuration} instance if {@code null} is returned.
   *
   * <p><strong>Note:</strong> Exceptions thrown by {@code loadInternal()} are not handled here and
   * will propagate to the caller.</p>
   */
  private Configuration loadInternalNonNull() {
    Configuration c = loadInternal();
    return (c != null) ? c : new Configuration();
  }
}
