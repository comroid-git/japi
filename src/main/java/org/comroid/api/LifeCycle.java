package org.comroid.api;

public interface LifeCycle extends Initializable, Terminatable {
    default void lateInitialize() throws Throwable {
    }

    default void earlyTerminate() throws Throwable {
    }
}
