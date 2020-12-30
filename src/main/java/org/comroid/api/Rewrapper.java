package org.comroid.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface Rewrapper<T> extends Supplier<@Nullable T> {
    Rewrapper<?> EMPTY = () -> null;

    static <T> Rewrapper<T> empty() {
        //noinspection unchecked
        return (Rewrapper<T>) EMPTY;
    }

    default boolean isNull() {
        return test(Objects::isNull);
    }

    default boolean isNonNull() {
        return test(Objects::nonNull);
    }

    static <T> Rewrapper<T> ofSupplier(final Supplier<T> selfSupplier) {
        return selfSupplier::get;
    }

    @Override
    @Nullable T get();

    default @NotNull Optional<T> wrap() {
        return Optional.ofNullable(get());
    }

    default Stream<T> stream() {
        if (isNull())
            return Stream.empty();
        return Stream.of(get());
    }

    default T assertion() throws AssertionError {
        return orElseThrow(AssertionError::new);
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

    default T orElseGet(Supplier<T> otherProvider) {
        if (isNull())
            return otherProvider.get();
        return requireNonNull("Assertion Failure");
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

    default <R> @Nullable R into(Class<R> type) {
        final T it = get();

        if (type.isInstance(it))
            return type.cast(it);
        return null;
    }

    default boolean test(Predicate<@Nullable ? super T> predicate) {
        return predicate.test(get());
    }

    default boolean testIfPresent(Predicate<@NotNull ? super T> predicate) {
        if (isNull())
            return false;
        return predicate.test(requireNonNull());
    }

    default boolean contentEquals(T other) {
        if (other == null)
            return isNull();
        return into(other::equals);
    }

    default void ifPresent(Consumer<T> consumer) {
        if (isNonNull())
            consume(consumer);
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
        if (isNonNull())
            return into(consumer);
        else return task.get();
    }
}