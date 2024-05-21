package org.comroid.api.tree;

public interface Terminatable {
    default void terminate() throws Throwable {
        if (this instanceof AutoCloseable ac)
            ac.close();
    }
}
