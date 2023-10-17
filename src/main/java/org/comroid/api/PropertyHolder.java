package org.comroid.api;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.comroid.api.Polyfill.uncheckedCast;

public interface PropertyHolder extends PropertiesHolder, UUIDContainer {
    @Override
    default UUID getUUID() {
        return this.<UUID>computeProperty("uuid", ($,id)->id==null?UUID.randomUUID():id).assertion();
    }

    default Map<String, Object> getPropertyCache() {
        return Support.getCache(this);
    }

    @Override
    default <T> SupplierX<T> computeProperty(String name, N.Function.$2<String, @Nullable T, @Nullable T> func) {
        return SupplierX.ofSupplier(()-> uncheckedCast(getPropertyCache().compute(name, uncheckedCast(func))));
    }

    @Override
    default <T> SupplierX<T> getProperty(String name) {
        return SupplierX.ofSupplier(() -> uncheckedCast(getPropertyCache().get(name)));
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
