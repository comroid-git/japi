package org.comroid.api;

import java.util.function.Function;

public interface ThrowingFunction<I, O, T extends Throwable> {
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
