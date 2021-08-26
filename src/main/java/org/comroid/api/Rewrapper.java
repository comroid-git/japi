package org.comroid.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * Pre-defining interface for Reference-like structures
 *
 * @param <T> The type of held data
 */
public interface Rewrapper<T> extends Supplier<@Nullable T>, Referent<T>, MutableState, StreamSupplier<T>, Upgradeable {
    Rewrapper<?> EMPTY = () -> null;

    @Override
    default boolean isMutable() {
        return false;
    }

    @Override
    default boolean isNull() {
        return test(Objects::isNull);
    }

    @Override
    default boolean isNonNull() {
        return test(Objects::nonNull);
    }

    static <T> Rewrapper<T> empty() {
        //noinspection unchecked
        return (Rewrapper<T>) EMPTY;
    }

    static <T> Rewrapper<T> ofSupplier(final Supplier<T> selfSupplier) {
        return selfSupplier::get;
    }

    static <T> Rewrapper<T> of(final T value) {
        if (value == null)
            return empty();
        return () -> value;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> Rewrapper<T> ofOptional(final Optional<? extends T> optional) {
        return () -> optional.orElse(null);
    }

    @Override
    default boolean setMutable(boolean state) {
        return false;
    }

    @Override
    @Nullable T get();

    default @NotNull Optional<T> wrap() {
        return Optional.ofNullable(get());
    }

    default Stream<? extends T> stream() {
        if (isNull())
            return Stream.empty();
        return Stream.of(get());
    }

    default T assertion() throws AssertionError {
        return orElseThrow(AssertionError::new);
    }

    default T assertion(String message) throws AssertionError {
        return orElseThrow(() -> new AssertionError(message));
    }

    default T assertion(Supplier<String> messageSupplier) throws AssertionError {
        return orElseThrow(() -> new AssertionError(messageSupplier.get()));
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

    default void consume(Consumer<T> consumer) {
        consumer.accept(get());
    }

    default <R> R into(Function<? super T, R> remapper) {
        return remapper.apply(get());
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

    default <X, R> Rewrapper<R> combine(Supplier<X> other, BiFunction<T, X, R> accumulator) {
        return () -> accumulator.apply(get(), other.get());
    }

    default <X, R> R accumulate(Supplier<X> other, BiFunction<T, X, R> accumulator) {
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
        return into(other::equals);
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

    default <R> @Nullable R ifPresentMap(Function<T, R> consumer) {
        if (isNonNull())
            return into(consumer);
        return null;
    }

    default <R> R ifPresentMapOrElseGet(Function<T, R> consumer, Supplier<R> task) {
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

    default Rewrapper<T> or(final Supplier<? extends T> orElse) {
        return () -> orElseGet(orElse);
    }

    default Rewrapper<T> orRef(final Supplier<Rewrapper<? extends T>> orElse) {
        return () -> orElseGet(orElse.get());
    }

    default Rewrapper<T> orOpt(final Supplier<Optional<? extends T>> orElse) {
        return orRef(() -> Rewrapper.ofOptional(orElse.get()));
    }
}
