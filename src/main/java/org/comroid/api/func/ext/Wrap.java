package org.comroid.api.func.ext;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.experimental.NonFinal;
import org.comroid.annotations.Ignore;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.MutableState;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.func.Provider;
import org.comroid.api.func.Referent;
import org.comroid.api.func.exc.ThrowingSupplier;
import org.comroid.api.func.util.Invocable;
import org.comroid.api.info.Log;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Pre-defining interface for Reference-like structures
 *
 * @param <T> The type of held data
 */
@Ignore
public interface Wrap<T> extends Supplier<@Nullable T>, Referent<T>, MutableState, StreamSupplier<T>, Convertible {
    Wrap<?> EMPTY = () -> null;

    static <T> Wrap<T> of(final T value) {
        if (value == null) return empty();
        return () -> value;
    }

    static <T> Wrap<T> empty() {
        //noinspection unchecked
        return (Wrap<T>) EMPTY;
    }

    @Deprecated
    static <T> Wrap<T> ofSupplier(final Supplier<T> selfSupplier) {
        return of(selfSupplier);
    }

    static <T> Wrap<T> of(final Supplier<T> selfSupplier) {
        return selfSupplier == null ? empty() : selfSupplier::get;
    }

    @Deprecated
    static <T> Wrap<T> ofStream(Stream<T> stream) {
        return of(stream);
    }

    static <T> Wrap<T> of(Stream<T> stream) {
        return ofOptional(stream.findAny());
    }

    @Deprecated
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> Wrap<T> ofOptional(final Optional<? extends T> optional) {
        return of(optional);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> Wrap<T> of(final Optional<? extends T> optional) {
        return () -> optional.orElse(null);
    }

    static <R extends DataNode, T extends Throwable> Wrap<R> exceptionally(ThrowingSupplier<R, T> supplier) {
        return exceptionally(supplier, t -> Log.at(Level.SEVERE, "An internal error occurred", t));
    }

    static <R extends DataNode, T extends Throwable> Wrap<R> exceptionally(
            @NotNull ThrowingSupplier<R, T> supplier, @NotNull Consumer<T> handler) {
        return () -> {
            try {
                return supplier.get();
            } catch (Throwable e) {
                handler.accept(Polyfill.uncheckedCast(e));
                return null;
            }
        };
    }

    static <O> Wrap<O> onDemand(Supplier<O> source) {
        return new Wrap<>() {
            private O object;

            @Override
            public O get() {
                if (object == null) object = source.get();
                return object;
            }
        };
    }

    @Override
    default boolean isMutable() {
        return false;
    }

    @Override
    default boolean setMutable(boolean state) {
        return false;
    }

    @Override
    default boolean isNull() {
        try {
            return get() == null;
        } catch (NullPointerException ignored) {
            return true;
        }
    }

    default @NotNull Optional<T> wrap() {
        try {
            return Optional.ofNullable(get());
        } catch (NullPointerException ignored) {
            return Optional.empty();
        }
    }

    default @NotNull T requireNonNull() throws NullPointerException {
        return Objects.requireNonNull(get());
    }

    default Stream<T> stream() {
        return Stream.of(get()).filter(Objects::nonNull);
    }

    default @NotNull T assertion(Supplier<String> messageSupplier) throws AssertionError {
        try {
            return orElseThrow(() -> new AssertionError(messageSupplier.get()));
        } catch (NullPointerException npe) {
            throw new AssertionError(messageSupplier.get(), npe);
        }
    }

    default <EX extends Throwable> T orElseThrow(Supplier<EX> exceptionSupplier) throws EX {
        if (isNull()) throw exceptionSupplier.get();
        return requireNonNull("Assertion Failure");
    }

    default @NotNull T requireNonNull(String message) throws NullPointerException {
        return Objects.requireNonNull(get(), message);
    }

    @Override
    @Nullable T get();

    default @NotNull T requireNonNull(Supplier<String> messageSupplier) throws NullPointerException {
        return Objects.requireNonNull(get(), messageSupplier);
    }

    @Contract("!null -> !null; _ -> _")
    default T orElse(T other) {
        if (isNull()) return other;
        return requireNonNull("Assertion Failure");
    }

    default T orElseThrow() throws NullPointerException {
        return orElseThrow(NullPointerException::new);
    }

    default Provider<T> provider() {
        return Provider.of(this);
    }

    default Invocable<T> invocable() {
        return Invocable.ofProvider(Provider.of(this));
    }

    default <R> R require(Function<? super @NotNull T, R> remapper) {
        return require(remapper, "Required value was not present");
    }

    default <R> R require(Function<? super @NotNull T, R> remapper, String message) {
        return remapper.apply(assertion(message));
    }

    default @NotNull T assertion(String message) throws AssertionError {
        try {
            return orElseThrow(() -> new AssertionError(message));
        } catch (NullPointerException npe) {
            throw new AssertionError(message, npe);
        }
    }

    /**
     * @deprecated Use {@link #cast(Class)}
     */
    @Deprecated
    default <R> @Nullable R into(Class<R> type) {
        return cast(type);
    }

    default <R> @Nullable R cast(Class<R> type) {
        final T it = get();

        if (type.isInstance(it)) return type.cast(it);
        return null;
    }

    @ApiStatus.Experimental
    default <R> Wrap<R> castRef() {
        return this::cast;
    }

    @ApiStatus.Experimental
    default <R> R cast() throws ClassCastException {
        return into(Polyfill::uncheckedCast);
    }

    default <R> @Nullable R into(Function<? super @NotNull T, R> remapper) {
        if (isNull()) return null;
        return remapper.apply(get());
    }

    default <X, R> @NotNull Wrap<@Nullable R> combine(
            @Nullable Supplier<@Nullable X> other, BiFunction<T, @Nullable X, @Nullable R> accumulator) {
        return () -> accumulate(other, accumulator);
    }

    default <X, R> @Nullable R accumulate(
            @Nullable Supplier<@Nullable X> other, BiFunction<T, X, @Nullable R> accumulator) {
        if (other == null || other.get() == null) return null;
        return accumulator.apply(get(), other.get());
    }

    default boolean contentEquals(Object other) {
        if (other == null) return isNull();
        return testIfPresent(other::equals);
    }

    default boolean testIfPresent(Predicate<@NotNull ? super T> predicate) {
        var value = get();
        if (value == null) return false;
        return predicate.test(value);
    }

    default void ifPresent(Consumer<T> consumer) {
        if (isNonNull()) consume(consumer);
    }

    default void consume(Consumer<@Nullable T> consumer) {
        consumer.accept(get());
    }

    default <EX extends Throwable> void ifPresentOrElseThrow(Consumer<T> consumer, Supplier<EX> exceptionSupplier)
    throws EX {
        if (isNonNull()) consume(consumer);
        else throw exceptionSupplier.get();
    }

    default void ifEmpty(Runnable task) {
        if (isNull()) task.run();
    }

    default void ifPresentOrElse(Consumer<T> consumer, Runnable task) {
        if (isNonNull()) consume(consumer);
        else task.run();
    }

    default <R> @Nullable R ifPresentMap(Function<? super T, ? extends R> consumer) {
        if (isNonNull()) return into(consumer);
        return null;
    }

    default <R> R ifPresentMapOrElseGet(Function<? super T, ? extends R> consumer, Supplier<R> task) {
        if (isNonNull()) {
            R into = into(consumer);
            if (into == null) return task.get();
            return into;
        } else return task.get();
    }

    default <R, X extends Throwable> R ifPresentMapOrElseThrow(Function<T, R> consumer, Supplier<X> exceptionSupplier)
    throws X {
        if (isNonNull()) return into(consumer);
        throw exceptionSupplier.get();
    }

    default <O> void ifBothPresent(@Nullable Supplier<O> other, BiConsumer<@NotNull T, @NotNull O> accumulator) {
        if (isNonNull() && other != null) {
            O o = other.get();
            if (o != null) accumulator.accept(assertion(), o);
        }
    }

    default @NotNull T assertion() throws AssertionError {
        try {
            return orElseThrow(AssertionError::new);
        } catch (NullPointerException npe) {
            throw new AssertionError("Assertion failure", npe);
        }
    }

    default <O, R> @Nullable R ifBothPresentMap(
            @Nullable Supplier<O> other, BiFunction<@NotNull T, @NotNull O, R> accumulator) {
        if (other != null) {
            O o = other.get();
            if (isNonNull() && o != null) return accumulator.apply(assertion(), o);
        }
        return null;
    }

    default Wrap<T> or(final Supplier<? extends T> orElse) {
        return () -> orElseGet(orElse);
    }

    default T orElseGet(Supplier<? extends T> otherProvider) {
        if (isNull()) return otherProvider.get();
        return requireNonNull("Assertion Failure");
    }

    default Wrap<T> orOpt(final Supplier<Optional<? extends T>> orElse) {
        return orRef(() -> Wrap.ofOptional(orElse.get()));
    }

    default Wrap<T> orRef(final Supplier<Supplier<? extends T>> orElse) {
        return () -> orElseGet(orElse.get());
    }

    default Wrap<T> peek(final Consumer<@NotNull T> action) {
        return map(x -> {
            action.accept(x);
            return x;
        });
    }

    default <O> Wrap<O> map(final Function<@NotNull T, @Nullable O> mapper) {
        return () -> into(mapper);
    }

    default Wrap<T> filter(final Predicate<@NotNull T> predicate) {
        return () -> test(predicate) ? get() : null;
    }

    default boolean test(Predicate<@Nullable ? super T> predicate) {
        return predicate.test(get());
    }

    default <O> Wrap<O> flatMap(final @NotNull Class<O> type) {
        return () -> test(type::isInstance) ? cast() : null;
    }

    default <O> Wrap<O> flatMap(final @NotNull Function<? super T, Supplier<? extends O>> func) {
        return () -> {
            Supplier<? extends O> supplier = ifPresentMapOrElseGet(func, () -> null);
            return supplier == null ? null : supplier.get();
        };
    }

    @Value
    @NoArgsConstructor
    class Lazy<T> implements Wrap<T> {
        public static <T> Lazy<T> lazy(Supplier<T> supplier) {
            var lazy = new Lazy<T>();
            CompletableFuture.supplyAsync(supplier).thenAcceptAsync(lazy::set);
            return lazy;
        }

        @NonFinal T       value;
        @NonFinal boolean set;

        @Override
        public boolean isNull() {
            return !set || value == null;
        }

        @Override
        public T get() {
            if (!set) throw new UnsupportedOperationException("Value has not been set yet");
            return value;
        }

        public Lazy<T> set(T value) {
            if (set) throw new UnsupportedOperationException("Value has already been set");
            this.value = value;
            this.set   = true;
            return this;
        }
    }

    @Value
    class Future<T> implements Wrap<T> {
        @Delegate                  CompletableFuture<T> future;
        @NonFinal @Setter @NotNull Level                logLevel = Level.FINE;

        public Future() {
            this(new CompletableFuture<>());
        }

        public Future(CompletableFuture<T> future) {
            this.future = future;
        }

        @Override
        public T get() {
            try {
                try {
                    return future.get(0, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    Log.at(logLevel, "Resource was not ready: " + future);
                    return future.join();
                }
            } catch (CompletionException | ExecutionException | InterruptedException e) {
                Log.at(logLevel, "Failed to get resource: " + future, e);
                return null;
            }
        }
    }
}
