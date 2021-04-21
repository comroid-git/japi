package org.comroid.api;

import java.util.Objects;
import java.util.stream.Stream;

public interface StreamSupplier<T> extends Upgradeable<StreamSupplier<T>> {
    @SafeVarargs
    static <T> StreamSupplier<T> of(final T... values) {
        return () -> Stream.of(values);
    }

    static <T> StreamSupplier<T> concat(final StreamSupplier<? extends T> first, final StreamSupplier<? extends T> second) {
        Objects.requireNonNull(first, "First Supplier");
        Objects.requireNonNull(second, "Second Supplier");

        return () -> Stream.concat(first.stream(), second.stream());
    }

    Stream<T> stream();

    default StreamSupplier<T> append(final StreamSupplier<? extends T> other) {
        return concat(this, other);
    }
}
