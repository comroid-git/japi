package org.comroid.api.func;

import java.util.concurrent.CompletableFuture;

/**
 * A builder-like interface which returns a {@link CompletableFuture} to complete with the product, instead of the finished object.
 *
 * @param <T> The type of the result object
 */
public interface Composer<T> extends Provider<T> {
    @Override
    default CompletableFuture<T> get() {
        return compose();
    }

    /**
     * Starts computation for the creation of the object.
     *
     * @return A future to complete with the final object.
     */
    CompletableFuture<T> compose();
}
