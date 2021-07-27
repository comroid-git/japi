package org.comroid.util;

import java.util.Map;
import java.util.Optional;
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
}
