package org.comroid.api;

import java.util.function.Function;
import java.util.function.Predicate;

public interface ThrowingPredicate<R, T extends Throwable> {
    static <R, T extends Throwable> Predicate<R> rethrowing(
            ThrowingPredicate<R, T> predicate,
            Function<Throwable, ? extends RuntimeException> remapper
    ) {
        return in -> {
            try {
                return predicate.test(in);
            } catch (Throwable error) {
                throw remapper.apply(error);
            }
        };
    }

    boolean test(R it) throws T;
}
