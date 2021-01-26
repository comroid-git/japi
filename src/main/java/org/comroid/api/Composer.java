package org.comroid.api;

import java.util.concurrent.CompletableFuture;

public interface Composer<T> extends Provider<T> {
    CompletableFuture<T> compose();

    @Override
    default CompletableFuture<T> get() {
        return compose();
    }
}
