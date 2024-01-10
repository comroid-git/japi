package org.comroid.api;

import org.comroid.api.func.N;
import org.comroid.api.func.ext.Wrap;
import org.jetbrains.annotations.Nullable;

@Deprecated
public interface PropertiesHolder {
    <T> Wrap<T> computeProperty(String name, N.Function.$2<String, @Nullable T, @Nullable T> func);

    <T> Wrap<T> getProperty(String name);

    boolean setProperty(String name, Object value);
}
