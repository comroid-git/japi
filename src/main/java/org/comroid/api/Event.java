package org.comroid.api;

import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.comroid.api.Polyfill.uncheckedCast;
import static org.comroid.util.StackTraceUtils.caller;

@Data
@EqualsAndHashCode(of = {"cancelled", "unixNanos", "key", "seq"})
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Event<T> implements Rewrapper<T> {
    long unixNanos = System.nanoTime();
    long seq;
    @Nullable String key;
    T data;
    @NonFinal
    boolean cancelled = false;

    public boolean cancel() {
        return !cancelled && (cancelled = true);
    }

    @Override
    public @Nullable T get() {
        return data;
    }

    public @NotNull Rewrapper<String> wrapKey() {
        return this::getKey;
    }

    public Instant getTimestamp() {
        return Instant.ofEpochMilli(unixNanos / 1000);
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static abstract class Factory<T, E extends Event<? super T>> implements N.Function.$2<T, String, E> {
        @NotNull AtomicLong counter = new AtomicLong(0);

        public Long counter() {
            var seq = counter.addAndGet(1);
            if (seq == Long.MAX_VALUE)
                counter.set(0);
            return seq;
        }

        @Override
        public E apply(T data, String key) {
            return factory(counter(), key, data);
        }

        public abstract E factory(long seq, String key, T data);
    }

    @Value
    @EqualsAndHashCode(exclude = "bus")
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @ToString(of = "location", includeFieldNames = false)
    public static class Listener<T> implements Predicate<Event<T>>, Consumer<Event<T>>, Comparable<Listener<?>>, Closeable, Named {
        @NotNull
        static Comparator<Listener<?>> Comparator = java.util.Comparator.<Listener<?>>comparingInt(Listener::getPriority).reversed();
        @NotNull Event.Bus<T> bus;
        @Delegate
        Predicate<Event<T>> requirement;
        @Delegate
        Consumer<Event<T>> action;
        String location;
        @NonFinal
        @Setter
        int priority = 0;
        @NonFinal
        @Setter
        @Nullable
        String name;
        @NonFinal
        boolean active = true;

        private Listener(@Nullable String key, @NotNull Bus<T> bus, Predicate<Event<T>> requirement, Consumer<Event<T>> action) {
            this.name = key;
            this.bus = bus;
            this.requirement = requirement;
            this.action = action;
            this.location = caller(2);
        }

        @Override
        public void close() {
            active = false;
            synchronized (bus.listeners) {
                bus.listeners.remove(this);
            }
        }

        @Override
        public int compareTo(@NotNull Event.Listener<?> other) {
            return Comparator.compare(this, other);
        }
    }

    public interface IBus<T> {
        <R extends T> Event.Listener<T> listen(Class<R> type, Consumer<Event<R>> action);

        <R extends T> Event.Listener<T> listen(@Nullable String key, Class<R> type, Consumer<Event<R>> action);

        Event.Listener<T> listen(Consumer<Event<T>> action);

        Event.Listener<T> listen(@Nullable String key, Consumer<Event<T>> action);

        Event.Listener<T> listen(Predicate<Event<T>> predicate, Consumer<Event<T>> action);

        Event.Listener<T> listen(@Nullable String key, Predicate<Event<T>> predicate, Consumer<Event<T>> action);

        <R extends T> CompletableFuture<Event<R>> next();

        <R extends T> CompletableFuture<Event<R>> next(Class<R> type);

        <R extends T> CompletableFuture<Event<R>> next(Class<R> type, @Nullable Duration timeout);

        <R extends T> CompletableFuture<Event<R>> next(Predicate<Event<T>> requirement);

        <R extends T> CompletableFuture<Event<R>> next(Predicate<Event<T>> requirement, @Nullable Duration timeout);

        Event.Bus<T> peek(Consumer<T> action);

        Event.Bus<T> filter(Predicate<T> predicate);

        <R> Event.Bus<R> map(Function<T, @Nullable R> function);

        <R extends T> Event.Bus<R> flatMap(Class<R> type);
    }

    @Log
    @Getter
    @EqualsAndHashCode(of = {}, callSuper = true)
    @ToString(of = {"upstream", "factory", "active"})
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class Bus<O> extends Container.Base implements Named, N.Consumer.$2<O, String>, Provider<O>, UUIDContainer, IBus<O> {
        @Nullable
        @NonFinal
        Event.Bus<?> upstream;
        @NotNull Set<Event.Bus<?>> downstream = new HashSet<>();
        @NotNull Queue<Event.Listener<O>> listeners = new ConcurrentLinkedQueue<>();
        @Nullable
        @NonFinal
        Function<?, @Nullable O> function;
        @Nullable
        @NonFinal
        Function<String, String> keyFunction;
        @NonFinal
        @Setter
        Event.Factory<O, ? extends Event<O>> factory = new Factory<>() {
            @Override
            public Event<O> factory(long seq, String key, O data) {
                return new Event<>(seq, key, data);
            }
        };
        @NonFinal
        @Setter
        Executor executor = Context.wrap(Executor.class).orElseGet(() -> Executors.newFixedThreadPool(4));
        @NonFinal
        @Setter
        boolean active = true;
        @NonFinal
        @Setter
        String name = null;

        @Contract(value = "_ -> this", mutates = "this")
        public Event.Bus<O> setUpstream(@NotNull Event.Bus<? extends O> parent) {
            return setUpstream(parent, Function.identity());
        }

        @Contract(value = "_, _ -> this", mutates = "this")
        public <I> Bus<O> setUpstream(
                @NotNull Event.Bus<? extends I> parent,
                @NotNull Function<I, O> function
        ) {
            return setUpstream(parent, function, UnaryOperator.identity());
        }

        @Contract(value = "_, _, _ -> this", mutates = "this")
        public <I> Event.Bus<O> setUpstream(
                @NotNull Event.Bus<? extends I> parent,
                @NotNull Function<I, O> function,
                @Nullable Function<String, String> keyFunction
        ) {
            return setDependent(parent, function, keyFunction, true);
        }

        /*
        @Contract(value = "_, _ -> this", mutates = "this")
        public <I> Bus<O> setJunction(
                @NotNull Event.Bus<I> other,
                @NotNull Junction<I, O> junction
        ) {
            return setJunction(other, junction, null);
        }

        @Contract(value = "_, _, _ -> this", mutates = "this")
        public <I> Event.Bus<O> setJunction(
                @NotNull Event.Bus<I> other,
                @NotNull Junction<I, O> junction,
                @Nullable Junction<String, String> keyJunction
        ) {
            if (keyJunction == null)
                keyJunction = Junction.identity();
            other.setDependent(this, junction::backward, keyJunction::backward, false);
            return setDependent(other, junction::forward, keyJunction::forward, false);
        }
         */

        private <I> Bus<O> setDependent(
                final @NotNull Bus<? extends I> parent,
                final @NotNull Function<I,O> function,
                final @Nullable Function<String, String> keyFunction,
                final boolean cleanup
        ) {
            if (cleanup && this.upstream != null)
                this.upstream.downstream.remove(this);
            this.upstream = parent;
            this.function = function;
            this.keyFunction = keyFunction;
            this.upstream.downstream.add(this);
            return this;
        }

        public Bus() {
            this("EventBus @ " + caller(1));
        }

        public Bus(@Nullable String name) {
            this(null, null);
            this.name = name;
        }

        private Bus(@Nullable Event.Bus<? extends O> upstream) {
            this(upstream, Polyfill::uncheckedCast);
        }

        private <P> Bus(@Nullable Event.Bus<P> upstream, @Nullable Function<@NotNull P, @Nullable O> function) {
            this.upstream = upstream;
            this.function = function;

            if (upstream != null) upstream.downstream.add(this);
        }

        @Override
        public <R extends O> Listener<O> listen(Class<R> type, Consumer<Event<R>> action) {
            return listen(null, type, action);
        }

        @Override
        public <R extends O> Event.Listener<O> listen(@Nullable String key, final Class<R> type, Consumer<Event<R>> action) {
            return listen(key, e -> type.isInstance(e.getData()), uncheckedCast(action));
        }

        @Override
        public Listener<O> listen(Consumer<Event<O>> action) {
            return listen((String) null, action);
        }

        @Override
        public Event.Listener<O> listen(@Nullable String key, Consumer<Event<O>> action) {
            return listen(key, $ -> true, action);

        }

        @Override
        public Listener<O> listen(final Predicate<Event<O>> predicate, Consumer<Event<O>> action) {
            return listen(null, predicate, action);
        }

        @Override
        public Event.Listener<O> listen(final @Nullable String key, Predicate<Event<O>> predicate, Consumer<Event<O>> action) {
            Event.Listener<O> listener;
            if (action instanceof Event.Listener)
                listener = (Event.Listener<O>) action;
            else listener = new Event.Listener<>(key, this, predicate.and(e -> Objects.equals(e.key, key)), action);
            synchronized (listeners) {
                listeners.add(listener);
            }
            return listener;
        }

        @Override
        public <R extends O> CompletableFuture<Event<R>> next() {
            return next($ -> true);
        }

        @Override
        public <R extends O> CompletableFuture<Event<R>> next(final Class<R> type) {
            return next(type, null);
        }

        @Override
        public <R extends O> CompletableFuture<Event<R>> next(final Class<R> type, final @Nullable Duration timeout) {
            return next(e -> type.isInstance(e.getData()), timeout);
        }

        @Override
        public <R extends O> CompletableFuture<Event<R>> next(final Predicate<Event<O>> requirement) {
            return next(requirement, null);
        }

        @Override
        public <R extends O> CompletableFuture<Event<R>> next(final Predicate<Event<O>> requirement, final @Nullable Duration timeout) {
            final var future = new CompletableFuture<Event<R>>();
            final var listener = listen(e -> Optional.ofNullable(e)
                    .filter(requirement)
                    .filter($ -> !future.isDone())
                    .map(Polyfill::<Event<R>>uncheckedCast)
                    .ifPresent(future::complete));
            future.whenComplete((e, t) -> listener.close());
            if (timeout == null)
                return future;
            return future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public Event.Bus<O> peek(final Consumer<@NotNull O> action) {
            return filter(it -> {
                action.accept(it);
                return true;
            });
        }

        @Override
        public Event.Bus<O> filter(final Predicate<@NotNull O> predicate) {
            return map(x -> predicate.test(x) ? x : null);
        }

        @Override
        public <R> Event.Bus<R> map(final Function<@NotNull O, @Nullable R> function) {
            return new Event.Bus<>(this, function);
        }

        @Override
        public <R extends O> Event.Bus<R> flatMap(final Class<R> type) {
            return filter(type::isInstance).map(type::cast);
        }

        @Override
        public CompletableFuture<O> get() {
            return next().thenApply(Event::getData);
        }

        public Listener<O> log(final Logger log, final Level level) {
            return listen(e -> log.log(level, e.getData().toString()));
        }

        public Consumer<O> withKey(final String key) {
            return data -> publish(key, data);
        }

        public void publish(O data) {
            publish(null, data);
        }

        public void publish(String key, O data) {
            accept(data, key);
        }

        @Override
        public void accept(final O data, @Nullable final String key) {
            if (!active)
                return;
            executor.execute(() -> {
                try {
                    publish(factory.apply(data, key));
                    synchronized (downstream) {
                        for (var child : downstream)
                            child.$publishDownstream(data, key);
                    }
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Unable to publish event to " + this, t);
                }
            });
        }

        @Override
        public void closeSelf() {
            active = false;
            for (var listener : listeners)
                listener.close();
        }

        private void publish(Event<O> event) {
            synchronized (listeners) {
                //Collections.sort(listeners, Listener.Comparator);
                listeners.stream().sorted(Listener.Comparator).forEach(listener -> {
                    if (!listener.isActive() || event.isCancelled())
                        return;
                    else if (listener.test(event))
                        listener.accept(event);
                });
            }
        }

        private <P> void $publishDownstream(final P data, @Nullable final String key) {
            if (function == null)
                return;
            Function<@NotNull P, @Nullable O> func = uncheckedCast(function);
            var it = func.apply(data);
            if (it == null)
                return;
            accept(it, keyFunction == null ? key : keyFunction.apply(key));
        }
    }
}
