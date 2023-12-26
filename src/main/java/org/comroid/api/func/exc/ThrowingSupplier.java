package org.comroid.api.func.exc;

import org.comroid.api.Polyfill;
import org.comroid.api.func.ext.Wrap;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public interface ThrowingSupplier<T, E extends Throwable> {
    static <R, E extends Throwable> Supplier<@Nullable R> fallback(
            ThrowingSupplier<R, E> supplier
    ) {return fallback(supplier,null);}
    static <R, E extends Throwable> Supplier<@Nullable R> fallback(
            ThrowingSupplier<@Nullable R, E> supplier,
            @Nullable Function<Throwable, @Nullable R> fallback
    ) {
        return () -> {
            try {
                return supplier.get();
            } catch (Throwable t) {
                if (fallback == null)
                    return null;
                return fallback.apply(t);
            }
        };
    }

    static <R> Wrap<R> rethrowing(
            ThrowingSupplier<R, Throwable> supplier
    ) {
        return rethrowing(supplier, RuntimeException::new);
    }

    static <R, E extends Throwable> Wrap<R> rethrowing(
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

    default Supplier<T> wrap() {
        return wrap(null);
    }

    default Supplier<T> wrap(final @Nullable Function<E, ? extends RuntimeException> _remapper) {
        return () -> {
            try {
                return get();
            } catch (Throwable e) {
                var remapper = _remapper;
                if (remapper == null)
                    remapper = RuntimeException::new;
                throw remapper.apply(Polyfill.uncheckedCast(e));
            }
        };
    }
}
