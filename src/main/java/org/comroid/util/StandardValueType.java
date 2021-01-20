package org.comroid.util;

import org.comroid.api.Polyfill;
import org.comroid.api.ValueType;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public final class StandardValueType<R> implements ValueType<R> {
    public static final Set<ValueType<?>> values = new HashSet<>();
    public static final ValueType<Boolean> BOOLEAN
            = new StandardValueType<>(Boolean.class, "boolean", Boolean::parseBoolean);
    public static final ValueType<Character> CHARACTER
            = new StandardValueType<>(Character.class, "char", str -> str.toCharArray()[0]);
    public static final ValueType<Double> DOUBLE
            = new StandardValueType<>(Double.class, "double", Double::parseDouble);
    public static final ValueType<Float> FLOAT
            = new StandardValueType<>(Float.class, "float", Float::parseFloat);
    public static final ValueType<Integer> INTEGER
            = new StandardValueType<>(Integer.class, "int", Integer::parseInt);
    public static final ValueType<Long> LONG
            = new StandardValueType<>(Long.class, "long", Long::parseLong);
    public static final ValueType<Short> SHORT
            = new StandardValueType<>(Short.class, "short", Short::parseShort);
    public static final ValueType<String> STRING
            = new StandardValueType<>(String.class, "String", Function.identity());

    public static final ValueType<Void> VOID
            = new StandardValueType<>(Void.class, "Void", it -> null);
    public static final ValueType<Object> OBJECT
            = new StandardValueType<>(Object.class, "Object", it -> it);
    public static final ValueType<Object[]> ARRAY
            = new StandardValueType<>(Object[].class, "Array", it -> new Object[]{it});

    private final Class<R> type;
    private final String name;
    private final Function<String, R> converter;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public R parse(String data) {
        return converter.apply(data);
    }

    @Override
    public Class<R> getTargetClass() {
        return type;
    }

    public StandardValueType(Class<R> type, String name, Function<String, R> mapper) {
        this.type = type;
        this.name = name;
        this.converter = mapper;

        values.add(this);
    }

    public static <T> ValueType<T> typeOf(T value) {
        if (value == null)
            //noinspection unchecked
            return (ValueType<T>) StandardValueType.VOID;
        return values.stream()
                .filter(it -> it.test(value))
                .findAny()
                .map(Polyfill::<StandardValueType<T>>uncheckedCast)
                .orElse(null);
    }
}
