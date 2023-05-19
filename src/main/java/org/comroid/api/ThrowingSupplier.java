package org.comroid.api;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public interface ThrowingSupplier<T, E extends Throwable> {
    static <R> Supplier<R> rethrowing(
            ThrowingSupplier<R, Throwable> supplier
    ) {
        return rethrowing(supplier, RuntimeException::new);
    }

    static <R, E extends Throwable> Supplier<R> rethrowing(
            ThrowingSupplier<R, E> supplier,
            @Nullable Function<Throwable, ? extends RuntimeException> remapper
    ) {
        return () -> {
            try {
                return supplier.get();
            } catch (Throwable error) {
                if (remapper != null)
                    throw remapper.apply(error);
                return null;
            }
        };
    }

    T get() throws E;
}
