package org.comroid.api;

import lombok.SneakyThrows;

import java.util.function.Function;

public interface ThrowingFunction<I, O, T extends Throwable> {
    static <I, O> Function<I, O> sneaky(
            ThrowingFunction<I, O, Throwable> function
    ) {
        //noinspection Convert2Lambda,Anonymous2MethodRef
        return new Function<>() {
            @Override
            @SneakyThrows
            public O apply(I i) {
                return function.apply(i);
            }
        };
    }

    static <I, O> Function<I, O> rethrowing(
            ThrowingFunction<I, O, Throwable> function
    ) {
        return rethrowing(function, RuntimeException::new);
    }

    static <I, O, T extends Throwable> Function<I, O> rethrowing(
            ThrowingFunction<I, O, T> function,
            Function<Throwable, ? extends RuntimeException> remapper
    ) {
        return in -> {
            try {
                return function.apply(in);
            } catch (Throwable error) {
                if (remapper != null)
                    throw remapper.apply(error);
                return null;
            }
        };
    }

    O apply(I i) throws T;
}
