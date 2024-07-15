package org.comroid.api.func.util;

import lombok.Data;
import org.comroid.api.Polyfill;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@Data
public final class Cache<K, V> {
    private static final Map<Object, Object> cache = new ConcurrentHashMap<>();

    public static <T> T get(Supplier<T> source) {
        return get(source.getClass(), source);
    }

    public static <K, V> V get(K key, Supplier<V> source) {
        return Polyfill.uncheckedCast(cache.computeIfAbsent(key, $ -> source.get()));
    }

    public static <K, V> V compute(K key, BiFunction<@NotNull K, @Nullable V, @Nullable V> source) {
        // false positive
        //noinspection RedundantTypeArguments
        return Polyfill.uncheckedCast(cache.compute(key, (k, v) -> source.apply(Polyfill.<K>uncheckedCast(k), Polyfill.<V>uncheckedCast(v))));
    }

    private final Duration timeout;
}
