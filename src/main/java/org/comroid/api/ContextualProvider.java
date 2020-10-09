package org.comroid.api;

import org.jetbrains.annotations.NotNull;

public interface ContextualProvider<T> {
    @NotNull T getFromContext();

    interface This<T> extends ContextualProvider<T> {
        @Override
        @NotNull
        default T getFromContext() {
            return Polyfill.uncheckedCast(this);
        }
    }
}
