package org.comroid.api.data.seri;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.comroid.api.Polyfill;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class BoundValueType<T> implements ValueType<T> {
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

    public static <T> BoundValueType<T> of(Class<? super T> type) {
        return Polyfill.uncheckedCast($cache.computeIfAbsent(type, BoundValueType::new));
    }
}
