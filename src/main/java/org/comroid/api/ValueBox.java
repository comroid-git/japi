package org.comroid.api;

import org.jetbrains.annotations.NotNull;

public interface ValueBox<T> extends ValuePointer<T> {
    T getValue();
}
