package org.comroid.api.func.util;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Optional.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Optionals {
    public static <I, O> Function<I, Optional<O>> cast(Class<? extends O> type) {
        return x -> type.isInstance(x) ? of(type.cast(x)) : empty();
    }

    public static <T, A, R> Function<T, Optional<R>> combine(
            Optional<A> other,
            BiFunction<T, A, Optional<R>> combiner
    ) {
        return t -> other.flatMap(a -> combiner.apply(t, a));
    }

    private Optionals() {
        throw new AbstractMethodError("no");
    }
}
