package org.comroid.api;

import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.experimental.WithBy;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;

import static org.comroid.api.Polyfill.uncheckedCast;
import static org.comroid.util.StackTraceUtils.caller;

@Data
@EqualsAndHashCode(of = {"cancelled", "unixNanos", "key", "seq"})
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Event<T> implements Rewrapper<T> {
    long unixNanos = System.nanoTime();
    long seq;
    String key;
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

    public Instant getTimestamp() {
        return Instant.ofEpochMilli(unixNanos / 1000);
    }

    @FunctionalInterface
    public interface Factory<T, E extends Event<? super T>> extends N.Function.$3<T, String, Long, E> {
        @NotNull AtomicLong counter = new AtomicLong(0);

        @Override
        default Long getDefaultZ() {
            var seq = counter.addAndGet(1);
            if (seq == Long.MAX_VALUE)
                counter.set(0);
            return seq;
        }

        @Override
        default E apply(T data, String key, Long seq) {
            return factory(seq, key, data);
        }

        E factory(long seq, String key, T data);
    }

    @Value
    @EqualsAndHashCode(exclude = "bus")
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @ToString(of = "location", includeFieldNames = false)
    public static class Listener<T> implements Predicate<Event<T>>, Consumer<Event<T>>, Comparable<Listener<?>>, Closeable {
        @NotNull
        static Comparator<Listener<?>> Comparator = java.util.Comparator.<Listener<?>>comparingInt(Listener::getPriority).reversed();
        @NotNull Event.Bus<T> bus;
        @Delegate
        Predicate<Event<T>> requirement;
        @Delegate
        Consumer<Event<T>> action;
        @NotNull StackTraceElement location;
        @NonFinal
        @Setter
        int priority = 0;
        @NonFinal
        boolean active = true;

        private Listener(@NotNull Bus<T> bus, Predicate<Event<T>> requirement, Consumer<Event<T>> action) {
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

    @Value
    @Slf4j
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @ToString(of = {"parent", "factory", "active"})
    public static class Bus<T> implements N.Consumer.$2<T, String>, Provider<T>, Closeable {
        @Nullable
        @NonFinal
        Event.Bus<?> parent;
        @NotNull Set<Event.Bus<?>> children = new HashSet<>();
        @NotNull SortedSet<Event.Listener<T>> listeners = new ConcurrentSkipListSet<>(Listener.Comparator);
        @Nullable
        @NonFinal
        Function<?, @Nullable T> function;
        @Nullable
        @NonFinal
        Function<String, String> keyFunction;
        @NonFinal
        @Setter
        Event.Factory<T, ? extends Event<T>> factory = Event::new;
        @NonFinal
        @Setter
        Executor executor = Context.wrap(Executor.class).orElseGet(() -> Executors.newFixedThreadPool(4));
        @NonFinal
        @Setter
        boolean active = true;

        @Contract(value = "_ -> this", mutates = "this")
        public Event.Bus<T> setParent(@Nullable Event.Bus<? extends T> parent) {
            return setParent(parent, Function.identity());
        }

        @Contract(value = "_, _ -> this", mutates = "this")
        public <P> Bus<T> setParent(
                @Nullable Event.Bus<? extends P> parent,
                @NotNull Function<P, T> function
        ) {
            return setParent(parent, function, UnaryOperator.identity());
        }

        @Contract(value = "_, _, _ -> this", mutates = "this")
        public <P> Event.Bus<T> setParent(
                @Nullable Event.Bus<? extends P> parent,
                @NotNull Function<P, T> function,
                @Nullable Function<String, String> keyFunction
        ) {
            if (this.parent != null)
                this.parent.children.remove(this);
            this.parent = parent;
            this.function = function;
            this.keyFunction = keyFunction;
            if (this.parent != null)
                this.parent.children.add(this);
            return this;
        }

        public Bus() {
            this(null, null);
        }

        private Bus(@Nullable Event.Bus<? extends T> parent) {
            this(parent, Polyfill::uncheckedCast);
        }

        private <P> Bus(@Nullable Event.Bus<P> parent, @Nullable Function<@NotNull P, @Nullable T> function) {
            this.parent = parent;
            this.function = function;

            if (parent != null) parent.children.add(this);
        }

        public <R extends T> Event.Listener<T> listen(final Class<R> type, final Consumer<Event<R>> action) {
            return listen(e -> type.isInstance(e.getData()), uncheckedCast(action));
        }

        public Event.Listener<T> listen(final Consumer<Event<T>> action) {
            return listen($ -> true, action);
        }

        public Event.Listener<T> listen(final String key, final Consumer<Event<T>> action) {
            return listen(e -> e.key.equals(key), action);

        }

        public Event.Listener<T> listen(final Predicate<Event<T>> requirement, final Consumer<Event<T>> action) {
            Event.Listener<T> listener;
            if (action instanceof Event.Listener)
                listener = (Event.Listener<T>) action;
            else listener = new Event.Listener<>(this, requirement, action);
            synchronized (listeners) {
                listeners.add(listener);
            }
            return listener;
        }

        public <R extends T> CompletableFuture<Event<R>> next() {
            return next($ -> true);
        }

        public <R extends T> CompletableFuture<Event<R>> next(final Class<R> type) {
            return next(type, null);
        }

        public <R extends T> CompletableFuture<Event<R>> next(final Class<R> type, final @Nullable Duration timeout) {
            return next(e -> type.isInstance(e.getData()), timeout);
        }

        public <R extends T> CompletableFuture<Event<R>> next(final Predicate<Event<T>> requirement) {
            return next(requirement, null);
        }

        public <R extends T> CompletableFuture<Event<R>> next(final Predicate<Event<T>> requirement, final @Nullable Duration timeout) {
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

        public Event.Bus<T> filter(final Predicate<@NotNull T> predicate) {
            return map(x -> predicate.test(x) ? x : null);
        }

        public <R> Event.Bus<R> map(final Function<@NotNull T, @Nullable R> function) {
            return new Event.Bus<>(this, function);
        }

        public <R extends T> Event.Bus<R> flatMap(final Class<R> type) {
            return filter(type::isInstance).map(type::cast);
        }

        @Override
        public CompletableFuture<T> get() {
            return next().thenApply(Event::getData);
        }

        public Listener<T> log(final Logger log, final Level level) {
            return listen(e -> log.atLevel(level).log(e.getData().toString()));
        }

        @Override
        public void close() {
            active = false;
            for (var listener : listeners)
                listener.close();
        }

        @Override
        public void accept(final T data, @Nullable final String key) {
            if (!active)
                return;
            executor.execute(() -> {
                try {
                    publish(factory.apply(data, key));
                    synchronized (children) {
                        for (var child : children)
                            child.$publishDownstream(data, key);
                    }
                } catch (Throwable t) {
                    log.error("Unable to publish event to " + this, t);
                }
            });
        }

        private void publish(Event<T> event) {
            synchronized (listeners) {
                for (var listener : listeners)
                    if (!listener.isActive() || event.isCancelled())
                        break;
                    else if (listener.test(event))
                        listener.accept(event);
            }
        }

        private <P> void $publishDownstream(final P data, @Nullable final String key) {
            if (function == null)
                return;
            Function<@NotNull P, @Nullable T> func = uncheckedCast(function);
            var it = func.apply(data);
            if (it == null)
                return;
            accept(it, keyFunction == null ? key : keyFunction.apply(key));
        }
    }
}
