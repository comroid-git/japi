package org.comroid.api;

import org.jetbrains.annotations.Nullable;

public interface PropertiesHolder {
    <T> SupplierX<T> computeProperty(String name, N.Function.$2<String, @Nullable T, @Nullable T> func);

    <T> SupplierX<T> getProperty(String name);

    boolean setProperty(String name, Object value);
}
