package org.comroid.api.func;

import java.util.concurrent.CompletableFuture;

public interface Creator<T> extends Provider<T> {
    @Override
    default CompletableFuture<T> get() {
        return create();
    }

    CompletableFuture<T> create();
}
