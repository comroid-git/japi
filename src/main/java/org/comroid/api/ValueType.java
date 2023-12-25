package org.comroid.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.comroid.annotations.Ignore;

import javax.persistence.Transient;
import java.util.function.Function;
import java.util.function.Predicate;

public interface ValueType<R> extends ValuePointer<R>, Predicate<Object>, Named {
    @Ignore
    @Override
    @Transient
    @JsonIgnore
    @Deprecated
    default ValueType<R> getHeldType() {
        return this;
    }

    default boolean isNumeric() {
        return Number.class.isAssignableFrom(getTargetClass());
    }

    @Ignore
    @Transient
    @JsonIgnore
    @Deprecated
    default Function<String, R> getConverter() {
        return this::parse;
    }

    Class<R> getTargetClass();

    @Override
    default boolean test(Object it) {
        return getTargetClass().isInstance(it);
    }

    default <T> T convert(R value, ValueType<T> toType) {
        if (equals(toType))
            return Polyfill.uncheckedCast(value);
        if (value == null)
            return null;
        return toType.parse(value.toString());
    }

    R parse(String data);
}
