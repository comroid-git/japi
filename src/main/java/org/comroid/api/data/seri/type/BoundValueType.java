package org.comroid.api.data.seri.type;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.comroid.api.Polyfill;
import org.comroid.api.html.form.HtmlInputDesc;
import org.comroid.api.html.form.HtmlStringInputDesc;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@EqualsAndHashCode(of = "targetClass")
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class BoundValueType<T> implements ValueType<T>, HtmlStringInputDesc {
    private static final Map<Class<?>, BoundValueType<?>> $cache = new ConcurrentHashMap<>();
    public static final Map<Class<?>, BoundValueType<?>> cache = Collections.unmodifiableMap($cache);

    Class<T> targetClass;

    @Override
    public T parse(String data) {
        throw new AbstractMethodError("Cannot blindly parse " + targetClass.getCanonicalName());
    }

    @Override
    public String toString() {
        return "BoundValueType<%s>".formatted(targetClass.getCanonicalName());
    }

    public static <T> BoundValueType<T> of(Class<? extends T> type) {
        if (type.isEnum())
            return EnumValueType.of(Polyfill.uncheckedCast(type));
        return Polyfill.uncheckedCast($cache.computeIfAbsent(type, BoundValueType::new));
    }
}
