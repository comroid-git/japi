package org.comroid.api.data.seri.type;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.comroid.api.Polyfill;
import org.comroid.api.html.form.HtmlReadonlyStringInputDesc;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@EqualsAndHashCode(of = "targetClass")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class ArrayValueType<T> implements ValueType<T>, HtmlReadonlyStringInputDesc {
    private static final Map<Class<?>, ArrayValueType<?>> $cache = new ConcurrentHashMap<>();
    public static final Map<Class<?>, ArrayValueType<?>> cache = Collections.unmodifiableMap($cache);

    public static <T> ArrayValueType<T> of(Class<? extends T> type) {
        return Polyfill.uncheckedCast($cache.computeIfAbsent(type.arrayType(), ArrayValueType::new));
    }

    Class<T> targetClass;

    @Override
    public T parse(String data) {
        throw new AbstractMethodError("Cannot blindly parse " + targetClass.getCanonicalName());
    }

    @Override
    public @Nullable String[] getHtmlExtraAttributes() {
        return HtmlReadonlyStringInputDesc.super.getHtmlExtraAttributes();
    }

    @Override
    public String toString() {
        return "ArrayValueType<%s>".formatted(targetClass.getCanonicalName());
    }
}
