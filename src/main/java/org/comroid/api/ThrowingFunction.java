package org.comroid.api;

import java.util.function.Function;

public interface ThrowingFunction<I, O, T extends Throwable> {
    static <I, O, T extends Throwable> Function<I, O> handling(
            ThrowingFunction<I, O, T> function,
            Function<Throwable, ? extends RuntimeException> remapper
    ) {
        return in -> {
            try {
                return function.apply(in);
            } catch (Throwable error) {
                throw remapper.apply(error);
            }
        };
    }

    O apply(I i) throws T;
}
