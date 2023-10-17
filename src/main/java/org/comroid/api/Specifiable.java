package org.comroid.api;

import java.util.function.Supplier;

public interface Specifiable<B> extends SelfDeclared<B> {
    default <R extends B> R as(Class<R> type, String message) throws AssertionError {
        return as(type, () -> message);
    }

    default <R extends B> R as(Class<R> type, Supplier<String> message) throws AssertionError {
        return as(type).orElseThrow(() -> new AssertionError(message.get()));
    }

    default <R extends B> SupplierX<R> as(Class<R> type) {
        if (!isType(type))
            return SupplierX.empty();
        return SupplierX.of(type.cast(self().get()));
    }

    default boolean isType(Class<? extends B> type) {
        return type.isInstance(self().get());
    }
}
