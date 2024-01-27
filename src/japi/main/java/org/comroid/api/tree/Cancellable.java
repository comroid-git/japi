package org.comroid.api.tree;

public interface Cancellable {
    default void cancel() {
        cancel(false);
    }

    void cancel(boolean mayInterruptIfRunning);
}
