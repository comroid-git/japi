package org.comroid.api.tree;

public interface Terminatable {
    default void terminate() throws Throwable {
    }
}
