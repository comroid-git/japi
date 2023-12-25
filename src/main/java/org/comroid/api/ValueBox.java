package org.comroid.api;

import org.comroid.annotations.Ignore;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.Nullable;

public interface ValueBox<T> extends ValuePointer<T>, SupplierX<T> {
    @Ignore
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
