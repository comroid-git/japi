package org.comroid.api.map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.comroid.api.Polyfill;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class HashKeyMap<K, V> implements Map<K, V> {
    Set<Entry> entries = new HashSet<>();

    public HashKeyMap(Map<? extends K, ? extends V> map) {
        putAll(map);
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return entry(key.hashCode()).isPresent();
    }

    @Override
    public boolean containsValue(Object value) {
        return value != null && entries.stream().map(Entry::getValue).anyMatch(value::equals);
    }

    @Override
    public V get(Object key) {
        return entry(key.hashCode()).map(Entry::getValue).orElse(null);
    }

    @Override
    public @Nullable V put(K key, V value) {
        return entry(key.hashCode()).map(entry -> entry.value = value).orElseGet(() -> {
            var entry = new Entry(key, value);
            entries.add(entry);
            return entry.value;
        });
    }

    @Override
    public V remove(Object key) {
        var result = entry(key.hashCode());
        if (result.isEmpty()) return null;
        var entry = result.get();
        if (!entries.remove(entry)) return null;
        return entry.value;
    }

    @Override
    public void putAll(@NonNull Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        entries.clear();
    }

    @Override
    public @NonNull Set<K> keySet() {
        return entries.stream().map(Entry::getKey).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public @NonNull Collection<V> values() {
        return entries.stream().map(Entry::getValue).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public @NonNull Set<Map.Entry<K, V>> entrySet() {
        return Polyfill.uncheckedCast(entries);
    }

    private Optional<Entry> entry(int keyCode) {
        return entries.stream().filter(e -> e.hashCode() == keyCode).findAny();
    }

    @Value
    private class Entry implements Map.Entry<K, V> {
        K key;
        @NonFinal V value;

        @Override
        public V setValue(V value) {
            var previous = this.value;
            this.value = value;
            return previous;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        @SuppressWarnings("EqualsDoesntCheckParameterClass")
        public boolean equals(Object obj) {
            return obj != null && hashCode() == obj.hashCode();
        }
    }
}
