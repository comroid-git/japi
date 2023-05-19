package org.comroid.api;

import lombok.*;
import lombok.experimental.Delegate;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.Closeable;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;

import static org.comroid.api.Polyfill.uncheckedCast;
import static org.comroid.util.StackTraceUtils.caller;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Event<T> implements Rewrapper<T> {
    private final long unixTime = System.nanoTime();
    private final long seq;
    private final @NonNull T data;
    private boolean cancelled = false;

    public boolean cancel() {
        return !cancelled && (cancelled = true);
    }

    @Override
    public String toString() {
        return data.toString();
    }

    @Override
    public @Nullable T get() {
        return data;
    }

    @Data
    @NoArgsConstructor
    public static abstract class Factory<T, E extends Event<? super T>> implements Function<T, E> {
        private static final AtomicLong counter = new AtomicLong(0);

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

    @Data
    @ToString(of = "loc", includeFieldNames = false)
    public static final class Listener<T> implements Predicate<Event<T>>, Consumer<Event<T>>, Closeable {
        private final Event.Bus<T> bus;
        @Delegate
        private final Predicate<Event<T>> requirement;
        @Delegate
        private final Consumer<Event<T>> action;
        private final StackTraceElement loc;

        public Listener(Bus<T> bus, Predicate<Event<T>> requirement, Consumer<Event<T>> action) {
            this.bus = bus;
            this.requirement = requirement;
            this.action = action;
            this.loc = caller(2);
        }

        @Override
        public void close() {
            bus.listeners.remove(this);
        }
    }

    @Getter
    public static final class Bus<T> implements Consumer<T>, Supplier<T>, Closeable {
        private final @Nullable Event.Bus<?> parent;
        private final Set<Event.Bus<?>> children = new HashSet<>();
        private final @Nullable Function<?, @Nullable T> function;
        private final Set<Event.Listener<T>> listeners = new HashSet<>();
        private @Setter Event.Factory<T, ? extends Event<T>> factory = Event.Factory.$default();
        private @Setter Executor executor = Runnable::run;
        private @Setter boolean active = true;

        public Bus() {
            this(null, null);
        }

        public Bus(@Nullable Event.Bus<? extends T> parent) {
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
            listeners.add(listener);
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
                final var event = factory.apply(data);
                for (var listener : new HashSet<>(listeners))
                    if (event.isCancelled())
                        break;
                    else if (listener.test(event))
                        listener.accept(event);
                for (var child : children)
                    child.$publish(data);
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
