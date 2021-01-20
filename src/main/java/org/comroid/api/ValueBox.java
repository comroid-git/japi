package org.comroid.api;

public interface ValueBox<T> extends ValuePointer<T> {
    T getValue();
}
