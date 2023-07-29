package org.comroid.api;

public interface Reloadable extends Startable, Stoppable {
    default void reload() {
        stop();
        start();
    }
}
