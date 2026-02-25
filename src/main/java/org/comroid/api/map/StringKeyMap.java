package org.comroid.api.map;

import lombok.Value;
import org.comroid.api.Polyfill;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Value
public class StringKeyMap<V> implements Map<CharSequence, V> {
    Map<String, V> innerMap;

    public StringKeyMap() {
        this(Map.of());
    }

    public StringKeyMap(Map<? extends CharSequence, V> copy) {
        this(new ConcurrentHashMap<>(), copy);
    }

    public StringKeyMap(Map<String, V> innerMap, Map<? extends CharSequence, V> copy) {
        this.innerMap = innerMap;

        putAll(copy);
    }

    @Override
    public int size() {
        return innerMap.size();
    }

    @Override
    public boolean isEmpty() {
        return innerMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return innerMap.containsKey(key.toString());
    }

    @Override
    public boolean containsValue(Object value) {
        return innerMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return innerMap.get(key.toString());
    }

    @Override
    public @Nullable V put(CharSequence key, V value) {
        return innerMap.put(key.toString(), value);
    }

    @Override
    public V remove(Object key) {
        return innerMap.remove(key.toString());
    }

    @Override
    public void putAll(@NonNull Map<? extends CharSequence, ? extends V> other) {
        other.forEach(this::put);
    }

    @Override
    public void clear() {
        innerMap.clear();
    }

    @Override
    public @NonNull Set<CharSequence> keySet() {
        return Polyfill.uncheckedCast(innerMap.keySet());
    }

    @Override
    public @NonNull Collection<V> values() {
        return innerMap.values();
    }

    @Override
    public @NonNull Set<Entry<CharSequence, V>> entrySet() {
        return Polyfill.uncheckedCast(innerMap.entrySet());
    }
}
