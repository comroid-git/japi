package org.comroid.api;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public interface StreamSupplier<T> {
    static <T> StreamSupplier<T> empty() {
        return Stream::empty;
    }

    @SafeVarargs
    static <T> StreamSupplier<T> of(final T... values) {
        return () -> Stream.of(values);
    }

    @SafeVarargs
    static <T> StreamSupplier<T> ofNullable(final T @Nullable ... values) {
        if (values == null)
            return empty();
        return () -> Arrays.stream(values).filter(Objects::nonNull);
    }

    static <T> StreamSupplier<T> concat(final StreamSupplier<? extends T> first, final StreamSupplier<? extends T> second) {
        Objects.requireNonNull(first, "First Supplier");
        Objects.requireNonNull(second, "Second Supplier");

        return () -> Stream.concat(first.stream(), second.stream());
    }

    Stream<? extends T> stream();

    default StreamSupplier<T> append(final StreamSupplier<? extends T> other) {
        return concat(this, other);
    }
}
