package com.example.tetris.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.example.tetris.core.config.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("ConfigurationService")
class ConfigurationServiceTest {

  private ConfigurationService newService() {
    return mock(ConfigurationService.class,
        withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
  }

  @Test
  @DisplayName("Lazy cache: first getCache() loads once, subsequent calls reuse cached instance")
  void lazy_cache() {
    ConfigurationService svc = newService();
    when(svc.loadInternal()).thenReturn(null);

    Configuration c1 = svc.getCache();
    Configuration c2 = svc.getCache();

    assertNotNull(c1, "Public API must not return null");
    assertSame(c1, c2, "getCache() should return the same cached instance");
    verify(svc, times(1)).loadInternal();
  }

  @Test
  @DisplayName("load(): always refreshes from backing store even when cache is warm")
  void load_refreshes() {
    ConfigurationService svc = newService();
    Configuration first = new Configuration();
    Configuration second = new Configuration();

    when(svc.loadInternal()).thenReturn(first).thenReturn(second);

    assertSame(first, svc.getCache());
    assertSame(second, svc.load(), "load() should replace the cached value");
    assertSame(second, svc.getCache(), "Cache should now point to the freshly loaded value");
    verify(svc, times(2)).loadInternal();
  }

  @Test
  @DisplayName("Null from loadInternal → API substitutes new default Configuration")
  void null_load_substitutes_default() {
    ConfigurationService svc = newService();
    when(svc.loadInternal()).thenReturn(null);

    Configuration c = svc.getCache();
    assertNotNull(c);
    verify(svc, times(1)).loadInternal();
  }

  @Test
  @DisplayName("clearCache(): drops only memory cache; next getCache() triggers a fresh load")
  void clear_cache() {
    ConfigurationService svc = newService();
    when(svc.loadInternal()).thenReturn(new Configuration()).thenReturn(new Configuration());

    Configuration a = svc.getCache();
    svc.clearCache();
    Configuration b = svc.getCache();

    assertNotSame(a, b, "After clearCache(), next getCache() should reload");
    verify(svc, times(2)).loadInternal();
  }

  @Test
  @DisplayName("save(): delegates to saveInternal then replaces the cache with the saved instance")
  void save_delegates_updates_cache() {
    ConfigurationService svc = newService();
    when(svc.loadInternal()).thenReturn(new Configuration());
    svc.getCache();

    Configuration toSave = new Configuration();
    doAnswer(inv -> null).when(svc).saveInternal(toSave);

    svc.save(toSave);
    verify(svc, times(1)).saveInternal(toSave);
    assertSame(toSave, svc.getCache(), "Cache must be exactly the instance passed to save()");
  }

  @Test
  @DisplayName("save(): if saveInternal throws, exception propagates and cache remains unchanged")
  void save_throw_propagates_cache_unchanged() {
    ConfigurationService svc = newService();
    Configuration original = new Configuration();
    when(svc.loadInternal()).thenReturn(original);
    svc.getCache();

    Configuration failing = new Configuration();
    doThrow(new RuntimeException("Exception")).when(svc).saveInternal(failing);

    RuntimeException ex = assertThrows(RuntimeException.class, () -> svc.save(failing));
    assertEquals("Exception", ex.getMessage());
    assertSame(original, svc.getCache(), "Cache must still be the previous value after failure");
  }

  @ParameterizedTest(name = "Boot via {0} (true=getCache, false=load) results in same cached instance thereafter")
  @ValueSource(booleans = {true, false})
  @DisplayName("Boot path parameterized: getCache vs load")
  void boot_paths(boolean bootViaGetCache) {
    ConfigurationService svc = newService();
    Configuration boot = new Configuration();

    when(svc.loadInternal()).thenReturn(boot);

    Configuration first = bootViaGetCache ? svc.getCache() : svc.load();
    assertSame(boot, first);
    verify(svc, times(1)).loadInternal();

    Configuration again = svc.getCache();
    assertSame(boot, again);
    verify(svc, times(1)).loadInternal();
  }

  @Test
  @DisplayName("Exceptions from loadInternal propagate and do not set the cache")
  void load_exception_leaves_cache_unset() {
    ConfigurationService svc = newService();

    when(svc.loadInternal()).thenThrow(new RuntimeException("disk on fire"));

    RuntimeException ex = assertThrows(RuntimeException.class, svc::getCache);
    assertEquals("disk on fire", ex.getMessage());

    reset(svc);
    when(svc.loadInternal()).thenReturn(new Configuration());
    Configuration c = svc.getCache();
    assertNotNull(c);
    verify(svc, times(1)).loadInternal();
  }
}
