package org.comroid.api.func;

import org.comroid.annotations.Ignore;
import org.comroid.api.data.seri.type.StandardValueType;
import org.comroid.api.data.seri.type.ValueType;
import org.comroid.api.func.ext.Wrap;
import org.jetbrains.annotations.Nullable;

public interface ValueBox<T> extends ValuePointer<T>, Wrap<T> {
    @Override
    default ValueType<? extends T> getHeldType() {
        return into(StandardValueType::typeOf);
    }

    @Ignore
    T getValue();

    @Override
    @Nullable
    default T get() {
        return getValue();
    }
}
