package org.comroid.api.func.exc;

import java.util.function.Function;
import java.util.function.Predicate;

public interface ThrowingPredicate<R, T extends Throwable> {
    static <R, T extends Throwable> Predicate<R> swallowing(
            ThrowingPredicate<R, T> predicate
    ) {
        return rethrowing(predicate, nil -> null);
    }

    static <R, T extends Throwable> Predicate<R> rethrowing(
            ThrowingPredicate<R, T> predicate,
            Function<Throwable, ? extends RuntimeException> remapper
    ) {
        return in -> {
            try {
                return predicate.test(in);
            } catch (Throwable error) {
                RuntimeException exception = remapper.apply(error);
                if (exception == null)
                    return false;
                throw exception;
            }
        };
    }

    boolean test(R it) throws T;

    static <R> Predicate<R> rethrowing(
            ThrowingPredicate<R, Throwable> predicate
    ) {
        return rethrowing(predicate, RuntimeException::new);
    }
}
