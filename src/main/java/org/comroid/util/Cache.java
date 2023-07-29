package org.comroid.util;

import org.comroid.api.Polyfill;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class Cache<K, V> {
    private static final Map<Object, Object> cache = new ConcurrentHashMap<>();
    public static <T> T get(Supplier<T> source) {
        return get(source.getClass(), source);
    }

    public static <K,V> V get(K key, Supplier<V> source) {
        return Polyfill.uncheckedCast(cache.computeIfAbsent(key, $->source.get()));
    }
}
