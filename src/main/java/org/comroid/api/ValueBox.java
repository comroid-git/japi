package org.comroid.api;

import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.Nullable;

public interface ValueBox<T> extends ValuePointer<T>, SupplierX<T> {
    T getValue();

    @Override
    @Nullable
    default T get() {
        return getValue();
    }

    @Override
    default ValueType<? extends T> getHeldType() {
        return into(StandardValueType::typeOf);
    }
}
