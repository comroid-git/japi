package org.comroid.api;

public interface Stoppable extends UncheckedCloseable {
    default void stop() {
        close();
    }
}
