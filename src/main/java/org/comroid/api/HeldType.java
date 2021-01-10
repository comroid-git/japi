package org.comroid.api;

import java.util.function.Function;
import java.util.function.Predicate;

public interface HeldType<R> extends ValuePointer<R>, Predicate<Object>, Named {
    @Override
    @Deprecated
    default HeldType<R> getHeldType() {
        return this;
    }

    @Override
    default boolean test(Object it) {
        Class<R> targetClass = getTargetClass();
        Class<?> aClass = it.getClass();

        return targetClass.isAssignableFrom(aClass) && targetClass.isInstance(it);
    }

    default <T> T convert(R value, HeldType<T> toType) {
        if (equals(toType))
            return Polyfill.uncheckedCast(value);
        if (value == null)
            return null;
        return toType.parse(value.toString());
    }

    @Deprecated
    default Function<String, R> getConverter() {
        return this::parse;
    }

    R parse(String data);

    Class<R> getTargetClass();
}
