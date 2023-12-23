package org.comroid.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.comroid.api.DataStructure;
import org.comroid.api.Polyfill;
import org.comroid.api.ValueType;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BoundValueType<T> implements ValueType<T> {
    private static final Map<Class<?>, BoundValueType<?>> $cache = new ConcurrentHashMap<>();
    public static final Map<Class<?>, BoundValueType<?>> cache = Collections.unmodifiableMap($cache);

    Class<T> targetClass;
    DataStructure<T> struct = DataStructure.of(targetClass);

    @Override
    public T parse(String data) {
        return struct.construct(Map.of("data", data))
                .orElseThrow(() -> new AbstractMethodError("Cannot blindly parse " + targetClass.getCanonicalName()));
    }

    public static <T> BoundValueType<T> of(Class<? super T> type) {
        return Polyfill.uncheckedCast($cache.computeIfAbsent(type, BoundValueType::new));
    }
}
