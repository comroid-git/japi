package org.comroid.api.func;

import org.comroid.api.PropertiesHolder;
import org.comroid.api.attr.UUIDContainer;
import org.comroid.api.func.ext.Wrap;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.comroid.api.Polyfill.*;

public interface PropertyHolder extends PropertiesHolder, UUIDContainer {
    @Override
    default UUID getUuid() {
        return this.<UUID>computeProperty("uuid", ($, id) -> id == null ? UUID.randomUUID() : id).assertion();
    }

    default Map<String, Object> getPropertyCache() {
        return Support.getCache(this);
    }

    @Override
    default <T> Wrap<T> computeProperty(String name, N.Function.$2<String, @Nullable T, @Nullable T> func) {
        return Wrap.ofSupplier(() -> uncheckedCast(getPropertyCache().compute(name, uncheckedCast(func))));
    }

    @Override
    default <T> Wrap<T> getProperty(String name) {
        return Wrap.ofSupplier(() -> uncheckedCast(getPropertyCache().get(name)));
    }

    @Override
    default boolean setProperty(String name, Object value) {
        return getPropertyCache().put(name, value) != value;
    }

    @Internal
    final class Support {
        private static final Map<UUID, Map<String, Object>> cache = new ConcurrentHashMap<>();

        private static Map<String, Object> getCache(PropertyHolder holder) {
            return cache.computeIfAbsent(holder.getUuid(), k -> new ConcurrentHashMap<>());
        }
    }
}
