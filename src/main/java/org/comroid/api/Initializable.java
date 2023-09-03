package org.comroid.api;

import lombok.experimental.StandardException;

public interface Initializable {
    default void initialize() throws Throwable {
    }

    @StandardException
    class InitFailed extends RuntimeException {}
}
