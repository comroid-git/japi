package org.comroid.api.data.seri.type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Transient;
import org.comroid.annotations.Default;
import org.comroid.annotations.Ignore;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.Named;
import org.comroid.api.func.Specifiable;
import org.comroid.api.func.ValuePointer;
import org.comroid.api.html.form.HtmlFormElementDesc;
import org.comroid.api.java.ReflectionHelper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

public interface ValueType<R>
        extends ValuePointer<R>, Predicate<Object>, Named, HtmlFormElementDesc, Specifiable<ValueType<R>>,
        Default.Extension {
    static <T> ValueType<T> of(final Class<?> type) {
        return StandardValueType.forClass(type)
                .or(() -> type.isArray() ? ArrayValueType.of(type.getComponentType()) : null)
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

    Class<R> getTargetClass();

    default boolean isStandard() {
        return this instanceof StandardValueType<R>;
    }

    default boolean isBound() {
        return this instanceof BoundValueType<R>;
    }

    default boolean isArray() {
        return this instanceof ArrayValueType<R>;
    }

    @Ignore
    @Transient
    @JsonIgnore
    @Deprecated
    default Function<String, R> getConverter() {
        return this::parse;
    }

    @Override
    default @Nullable String[] getHtmlExtraAttributes() {
        return new String[0];
    }

    @Contract("null -> null; !null -> _")
    R parse(String data);

    @Override
    @Nullable
    default Object defaultValue() {
        return ReflectionHelper.fieldWithAnnotation(getTargetClass(), Default.class)
                .stream()
                .findAny()
                .map(fld -> ReflectionHelper.forceGetField(null, fld))
                .orElse(null);
    }

    @Override
    default boolean test(Object it) {
        return getTargetClass().isInstance(it);
    }

    default <T> T convert(R value, ValueType<T> toType) {
        if (equals(toType)) return Polyfill.uncheckedCast(value);
        if (value == null) return null;
        return toType.parse(value.toString());
    }
}
