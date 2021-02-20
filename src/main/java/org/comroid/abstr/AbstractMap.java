package org.comroid.abstr;

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
    default void putAll(@NotNull Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @NotNull
    @Override
    default Set<K> keySet() {
        return Collections.unmodifiableSet(entrySet().stream().map(Entry::getKey).collect(Collectors.toSet()));
    }

    @NotNull
    @Override
    default List<V> values() {
        return Collections.unmodifiableList(entrySet().stream().map(Entry::getValue).collect(Collectors.toList()));
    }
}
