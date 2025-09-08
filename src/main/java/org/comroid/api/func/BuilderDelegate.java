package org.comroid.api.func;

public interface BuilderDelegate<T, B> {
    B builder();

    T build(B builder);
}
