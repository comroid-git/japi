package org.comroid.api.func.util;

import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Optional.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Optionals {
    public static <I, O> Function<I, Optional<O>> cast(Class<? extends O> type) {
        return x -> type.isInstance(x) ? Optional.of(type.cast(x)) : empty();
    }

    public static <T, A, R> Function<T, Optional<R>> combine(Optional<A> other, BiFunction<T, A, Optional<R>> combiner) {
        return t -> other.flatMap(a -> combiner.apply(t, a));
    }

    public static OptionalInt of(@Nullable Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    public static OptionalLong of(@Nullable Long value) {
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    public static OptionalDouble of(@Nullable Double value) {
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    private Optionals() {
        throw new AbstractMethodError("no");
    }
}
