package org.comroid.api;

public interface Cancellable {
    default void cancel() {
        cancel(false);
    }

    void cancel(boolean mayInterruptIfRunning);
}
