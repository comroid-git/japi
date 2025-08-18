package org.comroid.api.func.exc;

public interface ThrowingIntFunction<R, T extends Throwable> {
    R apply(int in) throws T;
}
