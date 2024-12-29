package org.comroid.api.func.ext;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface Ref<T> extends Wrap<T>, Consumer<T> {
    @Override
    @Nullable T get();

    @Override
    void accept(@Nullable T value);

    default void set(@Nullable T value) {
        accept(value);
    }

    interface Delegated<T> extends Ref<T> {
        Ref<T> getDelegateRef();

        @Override
        @Nullable
        default T get() {
            return getDelegateRef().get();
        }

        @Override
        default void accept(@Nullable T value) {
            getDelegateRef().accept(value);
        }
    }
}
