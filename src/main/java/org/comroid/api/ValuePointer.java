package org.comroid.api;

public interface ValuePointer<T> {
    ValueType<? extends T> getHeldType();
}
