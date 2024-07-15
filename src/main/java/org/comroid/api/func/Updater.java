package org.comroid.api.func;

import java.util.concurrent.CompletableFuture;

public interface Updater<T> extends Provider<T> {
    @Override
    default CompletableFuture<T> get() {
        return update();
    }

    CompletableFuture<T> update();
}
