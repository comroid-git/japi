package org.comroid.api.tree;

public interface Reloadable extends Startable, Stoppable {
    default void reload() {
        stop();
        start();
    }
}
