package org.comroid.api.func.exc;

import org.comroid.exception.RethrownException;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface ThrowingBiFunction<A, B, R, T extends Throwable> {
    static <A, B, R> BiFunction<A, B, R> rethrowing(
            ThrowingBiFunction<A, B, R, Throwable> function
    ) {
        return rethrowing(function, RethrownException::new);
    }

    static <A, B, R, T extends Throwable> BiFunction<A, B, R> rethrowing(
            ThrowingBiFunction<A, B, R, T> function,
            Function<Throwable, ? extends RuntimeException> remapper
    ) {
        return (a, b) -> {
            try {
                return function.apply(a, b);
            } catch (Throwable error) {
                if (remapper != null)
                    throw remapper.apply(error);
                return null;
            }
        };
    }

    R apply(A a, B b) throws T;
}
