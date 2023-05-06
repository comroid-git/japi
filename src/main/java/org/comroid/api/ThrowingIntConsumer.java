package org.comroid.api;

public interface ThrowingIntConsumer<T extends Throwable> {
    void accept(int value) throws T;
}
