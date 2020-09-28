package org.comroid.api;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface ThrowingRunnable<T extends Throwable> {
    static <R, T extends Throwable> Runnable handling(
            ThrowingRunnable<T> throwingRunnable, @Nullable Function<T, ? extends RuntimeException> remapper
    ) {
        final Function<T, ? extends RuntimeException> finalRemapper = Polyfill.notnullOr(remapper,
                (Function<T, ? extends RuntimeException>) RuntimeException::new
        );

        return () -> {
            try {
                throwingRunnable.run();
            } catch (Throwable thr) {
                //noinspection unchecked
                throw finalRemapper.apply((T) thr);
            }
        };
    }

    void run() throws T;
}
