package org.comroid.api.func;

import org.comroid.annotations.Ignore;
import org.comroid.api.data.seri.ValueType;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.data.seri.StandardValueType;
import org.jetbrains.annotations.Nullable;

public interface ValueBox<T> extends ValuePointer<T>, Wrap<T> {
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
