package org.comroid.api;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface ThrowingConsumer<I, T extends Throwable> {
    @Deprecated(forRemoval = true)
    static <I, T extends Throwable> Consumer<I> handling(
            ThrowingConsumer<I, T> consumer,
            @Nullable Function<T, ? extends RuntimeException> remapper
    ) {
        return rethrowing(consumer, remapper);
    }

    static <I> Consumer<I> logging(Logger log, ThrowingConsumer<I, Throwable> action) {
        return it->{
            try {
                action.accept(it);
            } catch (Throwable e) {
                log.log(Level.WARNING, "An internal exception occurred", e);
            }
        };
    }

    static <I> Consumer<I> rethrowing(
            ThrowingConsumer<I, Throwable> consumer
    ) {
        return rethrowing(consumer, RuntimeException::new);
    }

    static <I, T extends Throwable> Consumer<I> rethrowing(
            ThrowingConsumer<I, T> consumer,
            @Nullable Function<T, ? extends RuntimeException> remapper
    ) {
        final Function<T, ? extends RuntimeException> finalRemapper = Polyfill.notnullOr(remapper,
                (Function<T, ? extends RuntimeException>) RuntimeException::new
        );

        return in -> {
            try {
                consumer.accept(in);
            } catch (Throwable thr) {
                //noinspection unchecked
                throw finalRemapper.apply((T) thr);
            }
        };
    }

    static <T extends Throwable> ThrowingConsumer<T, T> doThrow() {
        return t -> {
            throw t;
        };
    }

    default Consumer<I> log(Logger log) {
        return log(log, null);
    }

    default Consumer<I> log(Logger log, @Nullable String message) {
        return in -> {
            try {
                accept(in);
            } catch (Throwable t) {
                log.log(Level.SEVERE, Objects.requireNonNullElse(message, "An internal Exception occurred"), t);
            }
        };
    }

    default Consumer<I> wrap() {
        return wrap(null);
    }

    default Consumer<I> wrap(@Nullable Function<Throwable, ? extends RuntimeException> remapper) {
        return in -> {
            try {
                accept(in);
            } catch (Throwable error) {
                if (remapper != null)
                    throw remapper.apply(error);
            }
        };
    }

    void accept(I input) throws T;
}
