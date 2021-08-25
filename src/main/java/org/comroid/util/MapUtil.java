package org.comroid.util;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiPredicate;

public final class MapUtil {
    private MapUtil() {
        throw new UnsupportedOperationException();
    }

    public static <K, V> Optional<V> findValue(Map<K, V> map, K key, BiPredicate<K, K> keyTester) {
        for (Map.Entry<K, V> entry : map.entrySet())
            if (keyTester.test(key, entry.getKey()))
                return Optional.ofNullable(entry.getValue());
        return Optional.empty();
    }

    public static <K, V> Map<K, V> hashtable(Hashtable<K, V> hashtable) {
        return new AbstractMap<K, V>() {
            private final Hashtable<K, V> table = hashtable;

            @NotNull
            @Override
            public Set<Entry<K, V>> entrySet() {
                return table.entrySet();
            }

            @Override
            public V put(K key, V value) {
                return table.put(key, value);
            }
        };
    }
}
