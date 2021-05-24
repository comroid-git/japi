package org.comroid.api;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public interface ThrowingSupplier<T, E extends Throwable> {
    static <T, E extends Throwable> Supplier<T> rethrowing(
            ThrowingSupplier<T, E> supplier,
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
