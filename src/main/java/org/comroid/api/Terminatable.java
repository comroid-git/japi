package org.comroid.api;

public interface Terminatable {
    default void terminate() throws Throwable {
    }
}
