package org.comroid.api.func.exc;

public interface ThrowingIntSupplier<T extends Throwable> {
    int getAsInt() throws T;
}
