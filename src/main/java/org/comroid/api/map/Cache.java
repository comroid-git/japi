package org.comroid.api.map;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import org.comroid.api.func.util.Streams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.comroid.api.Polyfill.*;

@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Cache<K, V> extends AbstractMap<K, @Nullable V> {
    Map<K, Reference<V>>    map = new ConcurrentHashMap<>();
    ReferenceQueue<@Nullable V> queue = new ReferenceQueue<>();
    Function<@NotNull V, K> keyFunction;
    @Nullable @lombok.Builder.Default BiConsumer<K, @Nullable V>                     deleteCallback = null;
    @lombok.Builder.Default           BiFunction<V, ReferenceQueue<V>, Reference<V>> referenceCtor  = WeakReference::new;
    Function<K, Optional<V>> refresh;

    @Override
    public int size() {
        return (int) map.values().stream()
                .flatMap(ref -> Stream.ofNullable(ref.get()))
                .filter(Objects::nonNull)
                .count();
    }

    @Override
    public boolean containsKey(Object key0) {
        K key = uncheckedCast(key0);
        return map.get(key).get() != null;
    }

    public Optional<V> wrap(K key) {
        var ref   = map.get(key);
        V value = null;
        if (ref != null) {
            value = ref.get();
            ref.enqueue();
        }
        return Optional.ofNullable(value);
    }

    @Override
    public @Nullable V get(Object key0) {
        K key   = uncheckedCast(key0);
        V value = wrap(key).orElse(null);
        if (value != null) {
            var ref = referenceCtor.apply(value, queue);
            map.put(key, ref);
        }
        return value;
    }

    @Override
    public @Nullable V put(K key, V value) {
        return super.put(key, value);
    }

    @Override
    public void clear() {
        var keys = Stream.concat(
                map.entrySet().stream()
                        .map(Streams.Multi.mapB(Reference::get))
                        .flatMap(Streams.Multi.filterB(Objects::isNull))
                        .map(Entry::getKey),
                Streams.of(new Spliterator<Reference<? extends V>>() {
                            Reference<? extends V> ref;

                            @Override
                            public synchronized boolean tryAdvance(Consumer<? super Reference<? extends V>> consumer) {
                                if (ref == null || (ref = queue.poll()) == null)
                                    return false;
                                consumer.accept(ref);
                                return true;
                            }

                            @Override
                            public Spliterator<Reference<? extends V>> trySplit() {
                                return null;
                            }

                            @Override
                            public long estimateSize() {
                                return Long.MAX_VALUE;
                            }

                            @Override
                            public int characteristics() {
                                return NONNULL;
                            }
                        })
                        .map(Reference::get)
                        .filter(Objects::nonNull)
                        .map(keyFunction)
        ).toArray();
        for (Object key : keys) {
            var val = remove(key);
            if (deleteCallback != null)
                deleteCallback.accept(uncheckedCast(key), val);
        }
    }

    @NotNull
    @Override
    public Set<Entry<K, @NotNull V>> entrySet() {
        return map.entrySet().stream()
                .map(Streams.Multi.mapB(Reference::get))
                .flatMap(Streams.Multi.filterB(Objects::nonNull))
                .collect(Collectors.toUnmodifiableSet());
    }

    public V push(V value) {
        var key  = keyFunction.apply(value);
        var ref = map.put(key, referenceCtor.apply(value, queue));
        V   prev = null;
        if (ref != null) {
            prev = ref.get();
            if (prev != null)
                ref.enqueue();
        }
        return prev;
    }
}
