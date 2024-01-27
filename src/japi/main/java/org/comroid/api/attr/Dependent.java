package org.comroid.api.attr;

import org.jetbrains.annotations.Nullable;

public interface Dependent<T> {
    @Nullable T getDependent();
}
