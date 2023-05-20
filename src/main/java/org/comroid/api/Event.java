package org.comroid.api;

import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
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
@EqualsAndHashCode(of = "seq")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Event<T> implements Rewrapper<T> {
    long unixNanos = System.nanoTime();
    long seq;
    @NonNull T data;
    @NonFinal boolean cancelled = false;

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

    @Data
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static abstract class Factory<T, E extends Event<? super T>> implements Function<T, E> {
        @NotNull static AtomicLong counter = new AtomicLong(0);

        @Override
        public final E apply(T data) {
            var seq = counter.addAndGet(1);
            if (seq == Long.MAX_VALUE)
                counter.set(0);
            return create(seq, data);
        }

        public abstract E create(long seq, T data);

        public static <T, E extends Event<? super T>> Event.Factory<T,E> of(BiFunction<Long, T, E> factory) {
            return new Event.Factory<>() {
                @Override
                public E create(long seq, T data) {
                    return factory.apply(seq, data);
                }
            };
        }

        private static <T> Event.Factory<T,Event<T>> $default() {
            return of(Event::new);
        }
    }

    @Value
    @EqualsAndHashCode(exclude = "bus")
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @ToString(of = "location", includeFieldNames = false)
    public static class Listener<T> implements Predicate<Event<T>>, Consumer<Event<T>>, Comparable<Listener<?>>, Closeable {
        @NotNull static Comparator<Listener<?>> Comparator = java.util.Comparator.<Listener<?>>comparingInt(Listener::getPriority).reversed();
        @NotNull Event.Bus<T> bus;
        @Delegate Predicate<Event<T>> requirement;
        @Delegate Consumer<Event<T>> action;
        @NotNull StackTraceElement location;
        @NonFinal @Setter int priority = 0;
        @NonFinal boolean active = true;

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
    public static class Bus<T> implements Consumer<T>, Supplier<T>, Closeable {
        @Nullable @NonFinal Event.Bus<?> parent;
        @NotNull Set<Event.Bus<?>> children = new HashSet<>();
        @NotNull SortedSet<Event.Listener<T>> listeners = new ConcurrentSkipListSet<>(Listener.Comparator);
        @Nullable @NonFinal Function<?, @Nullable T> function;
        @NonFinal @Setter  Event.Factory<T, ? extends Event<T>> factory = Event.Factory.$default();
        @NonFinal @Setter Executor executor = Context.wrap(Executor.class).orElseGet(()->Executors.newFixedThreadPool(4));
        @NonFinal @Setter boolean active = true;

        @Contract(value = "_ -> this", mutates = "this")
        public Event.Bus<T> setParent(@Nullable Event.Bus<? extends T> parent) {
            return setParent(parent, Function.identity());
        }

        @Contract(value = "_, _ -> this", mutates = "this")
        public <P> Event.Bus<T> setParent(@Nullable Event.Bus<? extends P> parent, @NotNull Function<P, T> function) {
            if (this.parent != null)
                this.parent.children.remove(this);
            this.parent = parent;
            this.function = function;
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

        public Event.Listener<T> listen(final Consumer<Event<T>> action) {
            return listen($ -> true, action);
        }

        public <R extends T> Event.Listener<T> listen(final Class<R> type, final Consumer<Event<R>> action) {
            return listen(e -> type.isInstance(e.getData()), uncheckedCast(action));
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
            return next($->true);
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
        public T get() {
            return next().thenApply(Event::getData).join();
        }

        @Override
        public void accept(final T data) {
            publish(data);
        }

        public void publish(final T data) {
            if (!active)
                return;
            executor.execute(() -> {
                try {
                    final var event = factory.apply(data);
                    synchronized (listeners) {
                        for (var listener : listeners)
                            if (!listener.isActive() || event.isCancelled())
                                break;
                            else if (listener.test(event))
                                listener.accept(event);
                    }
                    synchronized (children) {
                        for (var child : children)
                            child.$publish(data);
                    }
                } catch (Throwable t) {
                    log.error("Unable to publish event to "+this,t);
                }
            });
        }

        public Listener<T> log(final Logger log, final Level level) {
            return listen(e -> log.atLevel(level).log(e.getData().toString()));
        }

        @Override
        public void close() {
            active = false;
        }

        private <P> void $publish(P data) {
            if (function == null)
                return;
            Function<@NotNull P, @Nullable T> func = uncheckedCast(function);
            var it = func.apply(data);
            if (it == null)
                return;
            publish(it);
        }
    }
}
