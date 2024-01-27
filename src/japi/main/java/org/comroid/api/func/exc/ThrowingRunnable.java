package org.comroid.api.func.exc;

import org.comroid.api.Polyfill;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface ThrowingRunnable<T extends Throwable> {
    @Deprecated(forRemoval = true)
    static <T extends Throwable> Runnable handling(
            ThrowingRunnable<T> runnable,
            @Nullable Function<T, ? extends RuntimeException> remapper
    ) {
        return rethrowing(runnable, remapper);
    }
    static Runnable rethrowing(
            ThrowingRunnable<Throwable> runnable
    ) {
        return rethrowing(runnable, RuntimeException::new);
    }

    static <T extends Throwable> Runnable rethrowing(
            ThrowingRunnable<T> throwingRunnable,
            @Nullable Function<T, ? extends RuntimeException> remapper
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
