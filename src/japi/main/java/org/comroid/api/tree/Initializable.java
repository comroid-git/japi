package org.comroid.api.tree;

import lombok.experimental.StandardException;

public interface Initializable {
    default void initialize() throws Throwable {
    }

    @StandardException
    class InitFailed extends RuntimeException {}
}
