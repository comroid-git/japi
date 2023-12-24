package org.comroid.api;

import org.comroid.annotations.Ignore;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Pre-defining interface for Reference-like structures
 *
 * @param <T> The type of held data
 */
@Ignore
public interface SupplierX<T> extends Supplier<@Nullable T>, Referent<T>, MutableState, StreamSupplier<T>, Convertible {
    SupplierX<?> EMPTY = () -> null;

    @Override
    default boolean isMutable() {
        return false;
    }

    @Override
    default boolean isNull() {
        try {
            return test(Objects::isNull);
        } catch (NullPointerException ignored) {
            return true;
        }
    }

    @Override
    default boolean isNonNull() {
        try {
            return test(Objects::nonNull);
        } catch (NullPointerException ignored) {
            return false;
        }
    }

    static <T> SupplierX<T> empty() {
        //noinspection unchecked
        return (SupplierX<T>) EMPTY;
    }

    static <T> SupplierX<T> ofSupplier(final Supplier<T> selfSupplier) {
        return selfSupplier::get;
    }

    static <T> SupplierX<T> of(final T value) {
        if (value == null)
            return empty();
        return () -> value;
    }

    static <T> SupplierX<T> ofStream(Stream<T> stream) {
        return ofOptional(stream.findAny());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> SupplierX<T> ofOptional(final Optional<? extends T> optional) {
        return () -> optional.orElse(null);
    }

    @Override
    default boolean setMutable(boolean state) {
        return false;
    }

    @Override
    @Nullable T get();

    default @NotNull Optional<T> wrap() {
        try {
            return Optional.ofNullable(get());
        } catch (NullPointerException ignored) {
            return Optional.empty();
        }
    }

    default Stream<? extends T> stream() {
        return Stream.of(get()).filter(Objects::nonNull);
    }

    default T assertion() throws AssertionError {
        try {
            return orElseThrow(AssertionError::new);
        } catch (NullPointerException npe) {
            throw new AssertionError("Assertion failure", npe);
        }
    }

    default T assertion(String message) throws AssertionError {
        try {
            return orElseThrow(() -> new AssertionError(message));
        } catch (NullPointerException npe) {
            throw new AssertionError(message, npe);
        }
    }

    default T assertion(Supplier<String> messageSupplier) throws AssertionError {
        try {
            return orElseThrow(() -> new AssertionError(messageSupplier.get()));
        } catch (NullPointerException npe) {
            throw new AssertionError(messageSupplier.get(), npe);
        }
    }

    default T requireNonNull() throws NullPointerException {
        return Objects.requireNonNull(get());
    }

    default T requireNonNull(String message) throws NullPointerException {
        return Objects.requireNonNull(get(), message);
    }

    default T requireNonNull(Supplier<String> messageSupplier) throws NullPointerException {
        return Objects.requireNonNull(get(), messageSupplier);
    }

    default T orElse(T other) {
        if (isNull())
            return other;
        return requireNonNull("Assertion Failure");
    }

    default T orElseGet(Supplier<? extends T> otherProvider) {
        if (isNull())
            return otherProvider.get();
        return requireNonNull("Assertion Failure");
    }

    default T orElseThrow() throws NullPointerException {
        return orElseThrow(NullPointerException::new);
    }

    default <EX extends Throwable> T orElseThrow(Supplier<EX> exceptionSupplier) throws EX {
        if (isNull())
            throw exceptionSupplier.get();
        return requireNonNull("Assertion Failure");
    }

    default Provider<T> provider() {
        return Provider.of(this);
    }

    default Invocable<T> invocable() {
        return Invocable.ofProvider(Provider.of(this));
    }

    default void consume(Consumer<@Nullable T> consumer) {
        consumer.accept(get());
    }

    default <R> R into(Function<? super @Nullable T, R> remapper) {
        return remapper.apply(get());
    }

    default <R> R require(Function<? super @NotNull T, R> remapper) {
        return require(remapper, "Required value was not present");
    }

    default <R> R require(Function<? super @NotNull T, R> remapper, String message) {
        return remapper.apply(assertion(message));
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

        if (type.isInstance(it))
            return type.cast(it);
        return null;
    }

    @ApiStatus.Experimental
    default <R> R cast() throws ClassCastException {
        return into(Polyfill::uncheckedCast);
    }

    default <X, R> @NotNull SupplierX<@Nullable R> combine(@Nullable Supplier<@Nullable X> other, BiFunction<T, @Nullable X, @Nullable R> accumulator) {
        return () -> accumulate(other, accumulator);
    }

    default <X, R> @Nullable R accumulate(@Nullable Supplier<@Nullable X> other, BiFunction<T, X, @Nullable R> accumulator) {
        if (other == null || other.get() == null)
            return null;
        return accumulator.apply(get(), other.get());
    }

    default boolean test(Predicate<@Nullable ? super T> predicate) {
        return predicate.test(get());
    }

    default boolean testIfPresent(Predicate<@NotNull ? super T> predicate) {
        if (isNull())
            return false;
        return predicate.test(requireNonNull());
    }

    default boolean contentEquals(Object other) {
        if (other == null)
            return isNull();
        return testIfPresent(other::equals);
    }

    default void ifPresent(Consumer<T> consumer) {
        if (isNonNull())
            consume(consumer);
    }

    default <EX extends Throwable> void ifPresentOrElseThrow(Consumer<T> consumer, Supplier<EX> exceptionSupplier) throws EX {
        if (isNonNull())
            consume(consumer);
        else throw exceptionSupplier.get();
    }

    default void ifEmpty(Runnable task) {
        if (isNull())
            task.run();
    }

    default void ifPresentOrElse(Consumer<T> consumer, Runnable task) {
        if (isNonNull())
            consume(consumer);
        else task.run();
    }

    default <R> @Nullable R ifPresentMap(Function<? super T, ? extends R> consumer) {
        if (isNonNull())
            return into(consumer);
        return null;
    }

    default <R> R ifPresentMapOrElseGet(Function<? super T, ? extends R> consumer, Supplier<R> task) {
        if (isNonNull()) {
            R into = into(consumer);
            if (into == null)
                return task.get();
            return into;
        } else return task.get();
    }

    default <R, X extends Throwable> R ifPresentMapOrElseThrow(Function<T, R> consumer, Supplier<X> exceptionSupplier) throws X {
        if (isNonNull())
            return into(consumer);
        throw exceptionSupplier.get();
    }

    default <O> void ifBothPresent(@Nullable Supplier<O> other, BiConsumer<@NotNull T, @NotNull O> accumulator) {
        if (isNonNull() && other != null) {
            O o = other.get();
            if (o != null)
                accumulator.accept(assertion(), o);
        }
    }

    default <O, R> @Nullable R ifBothPresentMap(@Nullable Supplier<O> other, BiFunction<@NotNull T, @NotNull O, R> accumulator) {
        if (other != null) {
            O o = other.get();
            if (isNonNull() && o != null)
                return accumulator.apply(assertion(), o);
        }
        return null;
    }

    default SupplierX<T> or(final Supplier<? extends T> orElse) {
        return () -> orElseGet(orElse);
    }

    default SupplierX<T> orRef(final Supplier<Supplier<? extends T>> orElse) {
        return () -> orElseGet(orElse.get());
    }

    default SupplierX<T> orOpt(final Supplier<Optional<? extends T>> orElse) {
        return orRef(() -> SupplierX.ofOptional(orElse.get()));
    }

    default SupplierX<T> peek(final Consumer<@NotNull T> action) {
        return map(x -> {
            action.accept(x);
            return x;
        });
    }

    default SupplierX<T> filter(final Predicate<@NotNull T> predicate) {
        return () -> test(predicate) ? get() : null;
    }

    default <O> SupplierX<O> map(final Function<@NotNull T, @Nullable O> mapper) {
        return () -> into(mapper);
    }

    default <O> SupplierX<O> flatMap(final @NotNull Class<O> type) {
        return () -> test(type::isInstance) ? cast() : null;
    }

    default <O> SupplierX<O> flatMap(final @NotNull Function<? super T, Supplier<? extends O>> type) {
        return ifPresentMapOrElseGet(type, ()->null)::get;
    }
}
