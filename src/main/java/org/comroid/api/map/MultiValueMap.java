package org.comroid.api.map;

import lombok.Value;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Value
public class MultiValueMap<K, V> implements Map<K, V> {
    Map<K, Collection<V>> underlying;

    public MultiValueMap() {
        this(new ConcurrentHashMap<>());
    }

    public MultiValueMap(Map<K, Collection<V>> underlying) {
        this.underlying = underlying;
    }

    public MultiValueMap<K, V> immutableCopy() {
        var map = new HashMap<K, Collection<V>>();
        for (var entry : underlying.entrySet()) map.put(entry.getKey(), Collections.unmodifiableCollection(entry.getValue()));
        return new MultiValueMap<>(Collections.unmodifiableMap(map));
    }

    @Override
    public int size() {
        return underlying.values().stream().mapToInt(Collection::size).sum();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return underlying.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return values().stream().anyMatch(value::equals);
    }

    @Override
    public V get(Object key) {
        return underlying.get(key).stream().findAny().orElseThrow();
    }

    @Override
    public @Nullable V put(K key, V value) {
        var set  = underlying.computeIfAbsent(key, $ -> new ArrayList<>());
        var prev = set.stream().filter(value::equals).findAny().orElse(null);

        set.add(value);
        return prev;
    }

    @Override
    public V remove(Object key) {
        return Optional.ofNullable(underlying.remove(key)).stream().flatMap(Collection::stream).findAny().orElse(null);
    }

    @Override
    public void putAll(@NonNull Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        underlying.clear();
    }

    @Override
    public @NonNull Set<K> keySet() {
        return underlying.keySet();
    }

    @Override
    public @NonNull Collection<V> values() {
        return underlying.values().stream().flatMap(Collection::stream).toList();
    }

    @Override
    public @NonNull Set<Entry<K, V>> entrySet() {
        return underlying.entrySet()
                .stream()
                .flatMap(e -> e.getValue().stream().map(value -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), value)))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @SuppressWarnings("Java8MapForEach")
    public void forEach(BiConsumer<? super K, ? super V> action) {
        entrySet().forEach(e -> action.accept(e.getKey(), e.getValue()));
    }
}
