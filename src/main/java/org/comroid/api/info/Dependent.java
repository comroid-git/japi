package org.comroid.api.info;

import org.jetbrains.annotations.Nullable;

public interface Dependent<T> {
    @Nullable T getDependent();
}
