package org.comroid.api;

public interface ThrowingIntSupplier<T extends Throwable> {
    int getAsInt() throws T;
}
