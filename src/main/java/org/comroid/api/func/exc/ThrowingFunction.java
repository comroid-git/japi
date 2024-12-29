package org.comroid.api.func.exc;

import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    static <I, O> Function<I, @Nullable O> fallback(ThrowingFunction<I, O, ?> function) {
        return fallback(function, null);
    }

    static <I, O> Function<I, @Nullable O> fallback(ThrowingFunction<I, O, ?> function, @Nullable Supplier<O> fallback) {
        return x -> {
            try {
                return function.apply(x);
            } catch (Throwable ignored) {
                return fallback == null ? null : fallback.get();
            }
        };
    }

    static <I, O> Function<I, @Nullable O> logging(Logger log, ThrowingFunction<I, O, Throwable> action) {
        return logging(log, action, null);
    }

    static <I, O> Function<I, @Nullable O> logging(Logger log, ThrowingFunction<I, O, Throwable> action, @Nullable Supplier<@Nullable O> fallback) {
        return it -> {
            try {
                return action.apply(it);
            } catch (Throwable t) {
                log.log(Level.WARNING, "An internal exception occurred", t);
            }
            return fallback == null ? null : fallback.get();
        };
    }

    O apply(I i) throws T;
}
