package org.comroid.api.tree;

public interface Stoppable extends UncheckedCloseable {
    default void stop() {
        close();
    }
}
