package org.comroid.api;

public interface ThrowingSupplier<T, E extends Throwable> {
    T get() throws E;
}
