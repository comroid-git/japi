package org.comroid.api;

import java.util.Optional;
import java.util.function.Supplier;

public interface Specifiable<B extends Specifiable<? super B>> extends SelfDeclared<B> {
    default <R extends B> R as(Class<R> type, String message) throws AssertionError {
        return as(type, () -> message);
    }

    default <R extends B> R as(Class<R> type, Supplier<String> message) throws AssertionError {
        return as(type).orElseThrow(() -> new AssertionError(message.get()));
    }

    default <R extends B> Optional<R> as(Class<R> type) {
        if (!isType(type)) {
            return Optional.empty();
        }

        return Optional.of(type.cast(self()));
    }

    default boolean isType(Class<? extends B> type) {
        return type.isInstance(self());
    }
}
