package org.comroid.api;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ThrowingConsumer<I, T extends Throwable> {
    static <I, T extends Throwable> Consumer<I> handling(
            ThrowingConsumer<I, T> throwingRunnable, @Nullable Function<T, ? extends RuntimeException> remapper
    ) {
        final Function<T, ? extends RuntimeException> finalRemapper = Polyfill.notnullOr(remapper,
                (Function<T, ? extends RuntimeException>) RuntimeException::new
        );

        return in -> {
            try {
                throwingRunnable.accept(in);
            } catch (Throwable thr) {
                //noinspection unchecked
                throw finalRemapper.apply((T) thr);
            }
        };
    }

    void accept(I input) throws T;
}
