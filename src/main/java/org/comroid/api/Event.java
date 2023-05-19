package org.comroid.api;

import lombok.*;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.comroid.api.Polyfill.uncheckedCast;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Event<T> {
    public final long unixTime = System.nanoTime();
    public final @NonNull T data;
    public boolean cancelled = false;

    @Data
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Listener<T> implements Predicate<Event<T>>, Consumer<Event<T>>, Closeable {
        private final Bus<T> bus;
        @Delegate
        private final Predicate<Event<T>> requirement;
        @Delegate
        private final Consumer<Event<T>> action;

        @Override
        public void close() {
            bus.listeners.remove(this);
        }
    }

    @Getter
    public static final class Bus<T> implements Consumer<T>, Closeable {
        private final @Nullable Event.Bus<?> parent;
        private final Set<Event.Bus<?>> children = new HashSet<>();
        private final @Nullable Function<?, @Nullable T> function;
        private final Set<Listener<T>> listeners = new HashSet<>();
        private @Setter Executor executor = Runnable::run;
        private @Setter boolean active = true;

        public Bus() {
            this(null, null);
        }

        public Bus(@Nullable Bus<? extends T> parent) {
            this(parent, Polyfill::uncheckedCast);
        }

        private <P> Bus(@Nullable Event.Bus<P> parent, @Nullable Function<@NotNull P, @Nullable T> function) {
            this.parent = parent;
            this.function = function;

            if (parent != null) parent.children.add(this);
        }

        public Listener<T> listen(final Consumer<Optional<Event<T>>> action) {
            return listen($ -> true, x -> action.accept(Optional.ofNullable(x)));
        }

        public <R extends T> Listener<T> listen(final Class<R> type, final Consumer<Event<R>> action) {
            return listen(e -> type.isInstance(e.getData()), uncheckedCast(action));
        }

        public Listener<T> listen(final Predicate<Event<T>> requirement, final Consumer<Event<T>> action) {
            Listener<T> listener;
            if (action instanceof Listener)
                listener = (Listener<T>) action;
            else listener = new Listener<>(this, requirement, action);
            listeners.add(listener);
            return listener;
        }

        public <R extends T> CompletableFuture<R> next(final Class<R> type) {
            return next(type, null);
        }

        public <R extends T> CompletableFuture<R> next(final Class<R> type, final @Nullable Duration timeout) {
            return this.<R>next(e -> type.isInstance(e.getData()), timeout).thenApply(Event::getData);
        }

        public <R extends T> CompletableFuture<Event<R>> next(final Predicate<Event<T>> requirement) {
            return next(requirement, null);
        }

        public <R extends T> CompletableFuture<Event<R>> next(final Predicate<Event<T>> requirement, final @Nullable Duration timeout) {
            final var future = new CompletableFuture<Event<R>>();
            final var listener = listen(e -> e
                    .filter(requirement)
                    .map(Event::getData)
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
        public void accept(final T data) {
            publish(data);
        }

        public void publish(final T data) {
            if (!active)
                return;
            executor.execute(() -> {
                final var event = new Event<>(data);
                for (Listener<T> listener : listeners)
                    if (event.isCancelled())
                        break;
                    else if (listener.test(event))
                        listener.accept(event);
                for (Bus<?> child : children)
                    child.$publish(data);
            });
        }

        @Override
        public void close() {
            active = false;
        }

        private <P> void $publish(P data) {
            if (function == null)
                return;
            var func = Polyfill.<Function<@NotNull P, @Nullable T>>uncheckedCast(function);
            var it = func.apply(data);
            if (it == null)
                return;
            publish(it);
        }
    }
}
