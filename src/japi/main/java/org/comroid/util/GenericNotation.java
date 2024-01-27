package org.comroid.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

// shamelessly stolen, but unused
public abstract class GenericNotation<T> implements Comparable<GenericNotation<T>> {
    protected final Type type;

    public final Type getType() {
        return type;
    }

    protected GenericNotation() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof Class<?>) {
            throw new IllegalArgumentException("Internal error: TypeReference constructed without actual type information");
        }
        type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    @Override
    public final int compareTo(GenericNotation<T> o) {
        return 0;
    }
}
