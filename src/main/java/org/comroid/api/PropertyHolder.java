package org.comroid.api;

import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public interface PropertyHolder extends PropertiesHolder, UUIDContainer {
    default Map<String, Object> getPropertyCache() {
        return Support.getCache(this);
    }

    @Override
    default <T> Rewrapper<T> getProperty(String name) {
        return Rewrapper.ofSupplier(() -> Polyfill.uncheckedCast(getPropertyCache().get(name)));
    }

    @Override
    default boolean setProperty(String name, Object value) {
        return getPropertyCache().put(name, value) != value;
    }

    @Internal
    final class Support {
        private static final Map<UUID, Map<String, Object>> cache = new ConcurrentHashMap<>();

        private static Map<String, Object> getCache(PropertyHolder holder) {
            return cache.computeIfAbsent(holder.getUUID(), k -> new ConcurrentHashMap<>());
        }
    }
}
