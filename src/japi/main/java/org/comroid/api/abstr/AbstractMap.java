package org.comroid.api.abstr;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface AbstractMap<K, V> extends Map<K, V> {
    @Override
    default boolean isEmpty() {
        return size() == 0;
    }

    @Override
    default int size() {
        return entrySet().size();
    }

    @Override
    default void putAll(@NotNull Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    default boolean containsKey(Object key) {
        return keySet().stream().anyMatch(key::equals);
    }

    @Override
    default boolean containsValue(Object value) {
        return values().stream().anyMatch(value::equals);
    }

    @NotNull
    @Override
    default Set<K> keySet() {
        //noinspection SimplifyStreamApiCallChains
        return Collections.unmodifiableSet(entrySet()
                .stream()
                .map(Entry::getKey)
                .collect(Collectors.toSet()));
    }

    @NotNull
    @Override
    default List<V> values() {
        //noinspection SimplifyStreamApiCallChains
        return Collections.unmodifiableList(entrySet()
                .stream()
                .map(Entry::getValue)
                .collect(Collectors.toList()));
    }
}
