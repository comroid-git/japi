package org.comroid.api.data.seri;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.comroid.annotations.Ignore;
import org.comroid.api.Polyfill;
import org.comroid.api.func.ValuePointer;
import org.comroid.api.attr.Named;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Transient;
import java.util.function.Function;
import java.util.function.Predicate;

public interface ValueType<R> extends ValuePointer<R>, Predicate<Object>, Named {
    static <T> ValueType<T> of(final Class<?> type) {
        return StandardValueType.forClass(type)
                .or(() -> BoundValueType.of(type))
                .cast();
    }

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

    @Language(value = "HTML", prefix = "<input type=\"", suffix = "\">")
    default String getHtmlInputType() {
        return "text";
    }

    @Language(value = "HTML", prefix = "<input ", suffix = ">")
    default @Nullable String[] getHtmlInputAttributes() {
        return new String[0];
    }

    R parse(String data);
}
