package org.comroid.api;

import lombok.*;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
public class Event<T> implements Wrap<T> {
    long unixNanos = System.nanoTime();
    long seq;
    @Nullable Long flag;
    @Nullable String key;
    @Nullable T data;
    @NonFinal
    boolean cancelled = false;

    public <R> @Nullable Event<R> withData(@Nullable R data) {
        return data == null ? null : new Event<>(seq, flag, key, data).setCancelled(cancelled);
    }
    public <R> @Nullable Event<R> withDataBy(@NotNull Function<@Nullable T, @Nullable R> function) {
        return withData(function.apply(getData()));
    }

    public boolean cancel() {
        return !cancelled && (cancelled = true);
    }

    @Override
    public @Nullable T get() {
        return data;
    }

    public @NotNull Wrap<String> wrapKey() {
        return this::getKey;
    }

    public Instant getTimestamp() {
        return Instant.ofEpochMilli(unixNanos / 1000);
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Subscriber {
        String EmptyName = "@@@";
        long DefaultFlag = 0xffffffffffffffffL;

        String value() default EmptyName;
        long flag() default DefaultFlag;
        FlagMode mode() default FlagMode.BitwiseOr;

        enum FlagMode { Numeric, BitwiseOr, BitwiseNot }
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static abstract class Factory<T, E extends Event<? super T>> implements N.Function.$3<T, String, Long, E> {
        @NotNull AtomicLong counter = new AtomicLong(0);

        public Long counter() {
            var seq = counter.addAndGet(1);
            if (seq == Long.MAX_VALUE)
                counter.set(0);
            return seq;
        }

        @Override
        public E apply(T data, String key, Long flag) {
            return factory(counter(), data, key, flag);
        }

        public abstract E factory(long seq, T data, String key, long flag);
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @EqualsAndHashCode(exclude = "bus", callSuper = true)
    @ToString(of = "location", includeFieldNames = false)
    public static class Listener<T> extends Container.Base implements Predicate<Event<T>>, Consumer<Event<T>>, Comparable<Listener<?>>, Closeable, Named {
        @NotNull
        static Comparator<Listener<?>> Comparator = java.util.Comparator.<Listener<?>>comparingInt(Listener::getPriority).reversed();
        @NotNull Event.Bus<T> bus;
        @lombok.experimental.Delegate
        Predicate<Event<T>> requirement;
        @lombok.experimental.Delegate
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
        public void closeSelf() {
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

    @Log
    @Getter
    @EqualsAndHashCode(of = {}, callSuper = true)
    @ToString(of = {"name", "upstream", "factory", "active"})
    public static class Bus<T> extends Container.Base implements Named, N.Consumer.$3<T, String, Long>, Provider<T>, UUIDContainer {
        @Nullable
        private Event.Bus<?> upstream;
        @NotNull Set<Event.Bus<?>> downstream = new HashSet<>();
        @NotNull Queue<Event.Listener<T>> listeners = new ConcurrentLinkedQueue<>();
        @Nullable
        private Function<@NotNull Event<?>, @Nullable Event<T>> function;
        @Nullable
        private Function<String, String> keyFunction;
        @Setter
        private Event.Factory<T, ? extends Event<T>> factory = new Factory<>() {
            @Override
            public Event<T> factory(long seq, T data, String key, long flag) {
                return new Event<>(seq, flag, key, data);
            }
        };
        @Setter
        //private Executor executor = Context.wrap(Executor.class).orElseGet(()->Runnable::run);
        private Executor executor = Context.wrap(Executor.class).orElseGet(()->Executors.newFixedThreadPool(4));
        @Setter
        private boolean active = true;
        @Setter
        private String name = null;

        @Contract(value = "_ -> this", mutates = "this")
        public Event.Bus<T> setUpstream(@NotNull Event.Bus<? extends T> parent) {
            return setUpstream(parent, Function.identity());
        }

        @Contract(value = "_, _ -> this", mutates = "this")
        public <I> Bus<T> setUpstream(
                @NotNull Event.Bus<? extends I> parent,
                @NotNull Function<@NotNull Event<I>, @Nullable Event<T>> function
        ) {
            return setUpstream(parent, function, UnaryOperator.identity());
        }

        @Contract(value = "_, _, _ -> this", mutates = "this")
        public <I> Event.Bus<T> setUpstream(
                @NotNull Event.Bus<? extends I> parent,
                @NotNull Function<@NotNull Event<I>, @Nullable Event<T>> function,
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

        private <I> Bus<T> setDependent(
                final @NotNull Bus<? extends I> parent,
                final @NotNull Function<@NotNull Event<I>, @Nullable Event<T>> function,
                final @Nullable Function<String, String> keyFunction,
                final boolean cleanup
        ) {
            if (cleanup && this.upstream != null)
                this.upstream.downstream.remove(this);
            this.upstream = parent;
            this.function = Polyfill.uncheckedCast(function);
            this.keyFunction = keyFunction;
            this.upstream.downstream.add(this);
            return this;
        }

        public Bus() {
            this("EventBus @ " + caller(1));
        }

        public Bus(@Nullable String name) {
            this(Objects.requireNonNullElseGet(name,()->"Event.Bus @ " + caller(3)), null);
            this.name = name;
        }

        private Bus(@Nullable Event.Bus<? extends T> upstream) {
            this("Event.Bus @ " + caller(1), upstream);
        }

        private Bus(@NotNull String name, @Nullable Event.Bus<? extends T> upstream) {
            this(name, upstream, Polyfill::uncheckedCast);
        }

        private <P> Bus(@Nullable Event.Bus<P> upstream, @Nullable Function<@NotNull Event<P>, @Nullable Event<T>> function) {
            this("Event.Bus @ " + caller(1), upstream, function);
        }

        private <P> Bus(@NotNull String name, @Nullable Event.Bus<P> upstream, @Nullable Function<@NotNull Event<P>, @Nullable Event<T>> function) {
            this.name = name;
            this.upstream = upstream;
            this.function = Polyfill.uncheckedCast(function);

            if (upstream != null)
                upstream.downstream.add(this);
            register(this);
        }

        public @Nullable Listener<T> register(Class<?> target) {
            return registerTargetListener(target, null);
        }

        public @Nullable Listener<T> register(Object target) {
            return registerTargetListener(target.getClass(), target);
        }

        private @Nullable Listener<T> registerTargetListener(Class<?> type, @Nullable Object target) {
            addChildren(target);

            //final var autoTypes = List.of(Event.class, String.class, Long.class, Instant.class);
            var subscribers = new HashSet<SubscriberImpl>();
            for (var method : type.getMethods()) {
                if ((target!=null&&!Modifier.isStatic(method.getModifiers())&&!method.canAccess(target)) || !method.isAnnotationPresent(Subscriber.class))
                    continue;
                var subscriber = method.getAnnotation(Subscriber.class);
                /*
                var unsatisfied = Arrays.stream(method.getParameterTypes())
                        .filter(Predicate.not(autoTypes::contains)).toArray();
                if (unsatisfied.length > 0) {
                    log.fine(String.format("Invalid subscriber %s, unsupported method parameter: %s", method, Arrays.toString(unsatisfied)));
                    continue;
                }
                 */
                subscribers.add(new SubscriberImpl(method, Invocable.ofMethodCall(target, method), subscriber));
            }

            if (subscribers.isEmpty())
                return null;
            var listener = new SubscriberListener(target, subscribers);
            synchronized (listeners) {
                listeners.add(listener);
            }
            return listener;
        }

        public Event.Bus.Filter<T> listen() {
            return new Filter<>(this);
        }

        @Data
        public static final class Filter<T> {
            @NotNull Event.Bus<T> bus;
            @Nullable String key;
            @Nullable Long flag;
            @Nullable Class<? extends T> type;
            @Nullable Predicate<Event<T>> predicate;
            @Nullable Duration timeout;

            private Predicate<Event<T>> filters() {
                return ((Predicate<Event<T>>)(e -> key == null || Subscriber.EmptyName.equals(key) || Objects.equals(e.key, key)))
                        .and(e -> flag == null || flag == Subscriber.DefaultFlag || Objects.equals(e.flag, flag))
                        .and(Objects.requireNonNullElse(predicate, $->true))
                        .and(e -> type == null || e.testIfPresent(type::isInstance));
            }

            public Listener<T> subscribeData(final @NotNull Consumer<T> action) {
                return subscribe(e->action.accept(e.getData()));
            }
            public Listener<T> subscribe(final @NotNull Consumer<Event<T>> action) {
                var listener = new Event.Listener<>(key, bus, filters(), action);
                synchronized (bus.listeners) {
                    bus.listeners.add(listener);
                }
                return listener;
            }

            public CompletableFuture<Event<T>> once() {
                final var future = new CompletableFuture<Event<T>>();
                final var filters = filters();
                final var listener = subscribe(e -> {
                    if (future.isDone() || !filters.test(e))
                        return;
                    future.complete(e);
                });
                future.whenComplete((e, t) -> listener.close());
                return timeout == null ? future : future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
            }
        }

        public Listener<T> subscribeData(final @NotNull Consumer<T> action) {
            return subscribe(e->action.accept(e.getData()));
        }
        public Listener<T> subscribe(final @NotNull Consumer<Event<T>> action) {
            return listen().subscribe(action);
        }

        public Event.Bus<T> peekData(final Consumer<@NotNull T> action) {
            return filterData(it -> {
                action.accept(it);
                return true;
            });
        }
        public Event.Bus<T> filterData(final Predicate<@NotNull T> predicate) {
            return mapData(x -> predicate.test(x) ? x : null);
        }
        public <R> Event.Bus<R> mapData(final @NotNull Function<T, @Nullable R> function) {
            return map(e->e.withDataBy(function));
        }
        public <R extends T> Event.Bus<R> flatMap(final Class<R> type) {
            return filterData(type::isInstance).mapData(type::cast);
        }

        public Event.Bus<T> peek(final Consumer<Event<@NotNull T>> action) {
            return filter(it -> {
                action.accept(it);
                return true;
            });
        }
        public Event.Bus<T> filter(final Predicate<Event<@NotNull T>> predicate) {
            return map(x -> predicate.test(x) ? x : null);
        }
        public <R> Event.Bus<R> map(final @NotNull Function<@NotNull Event<T>, @Nullable Event<R>> function) {
            return new Event.Bus<>(this, function);
        }

        @Override
        public CompletableFuture<T> get() {
            return listen().once().thenApply(Event::getData);
        }

        public Listener<T> log(final Logger log, final Level level) {
            return listen().subscribe(e -> log.log(level, String.valueOf(e.getData())));
        }

        public Consumer<T> withKey(final String key) {
            return data -> publish(key, data);
        }

        public Event<T> publish(@Nullable T data) {
            return publish(null, data);
        }

        public Event<T> publish(@Nullable String key, @Nullable T data) {
            return publish(key, null, data);
        }

        @Builder(builderClassName = "Publisher", buildMethodName = "publish", builderMethodName = "publisher")
        public Event<T> publish(@Nullable String key, @Nullable Long flag_, @Nullable T data) {
            if (!active)
                return null;
            final var flag = flag_ == null ? Subscriber.DefaultFlag : flag_;
            final var event = factory.apply(data, key, flag);
            accept(event);
            return event;
        }

        @Override
        @Deprecated
        public void accept(final @Nullable T data, final @Nullable String key, final @Nullable Long flag) {
            publish(key, flag, data);
        }

        public void accept(final @Nullable Event<T> event) {
            executor.execute(() -> {
                try {
                    publish(event);
                    synchronized (downstream) {
                        for (var child : downstream)
                            child.$publishDownstream(event);
                    }
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Error in event handler " + this, t);
                }
            });
        }

        @Override
        public void closeSelf() {
            active = false;
            for (var listener : listeners)
                listener.close();
            for (var bus : downstream)
                bus.close();
        }

        private void publish(Event<T> event) {
            synchronized (listeners) {
                //Collections.sort(listeners, Listener.Comparator);
                listeners.stream().sorted(Listener.Comparator).forEach(listener -> {
                    if (listener.isActive() && !event.isCancelled() && listener.test(event))
                        listener.accept(event);
                });
            }
        }

        private <P> void $publishDownstream(final Event<P> data) {
            if (function == null)
                return;
            Function<@NotNull Event<P>, @Nullable Event<T>> func = uncheckedCast(function);
            var it = func.apply(data);
            if (it == null)
                return;
            accept(it);
        }

        @Value
        private class SubscriberImpl implements Predicate<Event<T>>, BiConsumer<@Nullable Object, Event<T>> {
            Method method;
            Invocable<?> delegate;
            Subscriber subscriber;

            @Override
            public boolean test(Event<T> event) {
                var key = Optional.of(subscriber.value())
                        .filter(Predicate.not(Subscriber.EmptyName::equals))
                        .orElseGet(method::getName);
                return Wrap.of(event.flag).or(()->0xffff_ffff_ffff_ffffL).testIfPresent(this::testFlag)
                        && (Objects.equals(key, event.key) || ("null".equals(key)
                        && (Objects.isNull(event.key) || Subscriber.EmptyName.equals(event.key))));
            }

            public boolean testFlag(long x) {
                var y = subscriber.flag();
                switch (subscriber.mode()) {
                    case Numeric:
                        return x==y;
                    case BitwiseOr:
                        return (x&y)!=0;
                    case BitwiseNot:
                        return (x&~y)!=0;
                }
                throw new IllegalStateException("Unexpected value: " + subscriber.mode());
            }

            @Override
            public void accept(@Nullable Object target, Event<T> event) {
                delegate.autoInvoke(event, event.getKey(), event.getData(), event.getSeq(), event.getTimestamp());
            }
        }

        @Value
        @EqualsAndHashCode(callSuper = true)
        private class SubscriberListener extends Listener<T> {
            @Nullable Object target;
            HashSet<SubscriberImpl> subscribers;

            public SubscriberListener(
                    @Nullable Object target,
                    HashSet<SubscriberImpl> subscribers) {
                super(null, Bus.this, $->true, $->{});
                this.target = target;
                this.subscribers = subscribers;
            }

            @Override
            public void accept(Event<T> event) {
                for (var subscriber : subscribers)
                    if (subscriber.test(event))
                        subscriber.accept(target, event);
            }
        }
    }
}
