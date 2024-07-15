package org.comroid.api.map;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import org.comroid.api.func.util.Streams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.comroid.api.Polyfill.uncheckedCast;
import static org.comroid.api.func.util.Streams.Multi.filterB;
import static org.comroid.api.func.util.Streams.Multi.mapB;

@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Cache<K, V> extends AbstractMap<K, @Nullable V> {
    Map<K, Reference<V>> map = new ConcurrentHashMap<>();
    ReferenceQueue<V> queue = new ReferenceQueue<>();
    Function<@NotNull V, K> keyFunction;
    BiConsumer<K, @Nullable V> deleteCallback;
    @lombok.Builder.Default
    Mode<V, Reference<V>> mode = Mode.weak(queue);
    Function<K, Optional<V>> refresh;

    @Override
    public boolean containsKey(Object _key) {
        K key = uncheckedCast(_key);
        return map.get(key).get() != null;
    }

    @Override
    public int size() {
        return (int) map.values().stream()
                .flatMap(ref -> Stream.ofNullable(ref.get()))
                .filter(Objects::nonNull)
                .count();
    }

    @Override
    public @Nullable V get(Object _key) {
        K key = uncheckedCast(_key);
        var ref = map.get(key);
        var value = ref.get();
        if (value == null) {
            ref.enqueue();
            value = compute(key, (k, v) -> refresh.apply(k)
                    .orElse(null));
        }
        if (value != null) {
            ref = mode.referenceCtor.apply(value);
            map.put(key, ref);
        }
        return value;
    }

    public V push(V value) {
        var key = keyFunction.apply(value);
        var ref = map.get(key);
        var prev = ref.get();
        if (prev != null)
            ref.enqueue();
        return prev;
    }

    @Override
    public @Nullable V put(K key, V value) {
        return super.put(key, value);
    }

    @NotNull
    @Override
    public Set<Entry<K, @NotNull V>> entrySet() {
        return map.entrySet().stream()
                .map(mapB(Reference::get))
                .flatMap(filterB(Objects::nonNull))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void clear() {
        var keys = Stream.concat(
                map.entrySet().stream()
                        .map(mapB(Reference::get))
                        .flatMap(filterB(Objects::isNull))
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
        for (Object key : keys)
            deleteCallback.accept(uncheckedCast(key), remove(key));
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static final class Mode<V, R extends Reference<V>> {
        Function<V, Reference<V>> referenceCtor;

        public static <V> Mode<V, Reference<V>> soft(ReferenceQueue<V> queue) {
            return new Mode<>(referent -> new SoftReference<>(referent, queue));
        }

        public static <V> Mode<V, Reference<V>> weak(ReferenceQueue<V> queue) {
            return new Mode<>(referent -> new WeakReference<>(referent, queue));
        }

        public static <V> Mode<V, Reference<V>> phantom(ReferenceQueue<V> queue) {
            return new Mode<>(referent -> new PhantomReference<>(referent, queue));
        }
    }
}
