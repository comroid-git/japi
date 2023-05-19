package org.comroid.api;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ThrowingConsumer<I, T extends Throwable> {
    @Deprecated(forRemoval = true)
    static <I, T extends Throwable> Consumer<I> handling(
            ThrowingConsumer<I, T> consumer,
            @Nullable Function<T, ? extends RuntimeException> remapper
    ) {
        return rethrowing(consumer, remapper);
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

    void accept(I input) throws T;
}
