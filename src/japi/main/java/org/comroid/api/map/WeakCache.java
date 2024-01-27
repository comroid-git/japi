package org.comroid.api.map;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.comroid.api.func.ext.Wrap;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Deprecated // todo: broken
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WeakCache<K,V> extends AbstractMap<K,V> {
    Map<K, WeakEntry> map = new ConcurrentHashMap<>();
    @Getter Function<K, V> provider;

    public final V touch(final K key) {
        return map.computeIfAbsent(key, WeakEntry::new).touch();
    }

    @NotNull
    @Override
    public final Set<Entry<K, V>> entrySet() {
        return map.entrySet().stream()
                .map(e->new SimpleImmutableEntry<>(e.getKey(),e.getValue().touch()))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Value
    private class WeakEntry {
        K key;
        @NonFinal WeakReference<V> ref = null;

        public WeakEntry(K key) {
            this.key = key;
        }

        V touch() {
            return Wrap.of(ref)
                    .map(Reference::get)
                    .orRef(()-> Wrap.of(key)
                            .map(provider) // todo sometimes causes NPE
                            .peek(it -> ref = new WeakReference<>(it)))
                    .orElseThrow(() -> new AssertionError("Unable to touch WeakCache object with key " + key));
        }
    }
}
