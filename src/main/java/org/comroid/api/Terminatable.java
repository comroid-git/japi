package org.comroid.api;

import lombok.experimental.Wither;

public interface Terminatable {
    default void terminate() throws Throwable {
    }
}
