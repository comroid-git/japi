package org.comroid.util;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.api.ValueType;
import org.jetbrains.annotations.ApiStatus.Experimental;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;

public final class StandardValueType<R> implements ValueType<R> {
    public static final ValueType<Boolean> BOOLEAN;
    public static final ValueType<Byte> BYTE;
    public static final ValueType<Character> CHARACTER;
    public static final ValueType<Double> DOUBLE;
    public static final ValueType<Float> FLOAT;
    public static final ValueType<Integer> INTEGER;
    public static final ValueType<Long> LONG;
    public static final ValueType<Short> SHORT;
    public static final ValueType<String> STRING;
    public static final ValueType<UUID> UUID;

    public static final ValueType<Void> VOID;
    public static final ValueType<Object> OBJECT;
    public static final ValueType<Object[]> ARRAY;

    public static final ValueType<?>[] values;

    static {
        BOOLEAN = new StandardValueType<>(Boolean.class, "boolean", Boolean::parseBoolean);
        BYTE = new StandardValueType<>(Byte.class, "byte", Byte::parseByte);
        CHARACTER = new StandardValueType<>(Character.class, "char", str -> str.toCharArray()[0]);
        DOUBLE = new StandardValueType<>(Double.class, "double", Double::parseDouble);
        FLOAT = new StandardValueType<>(Float.class, "float", Float::parseFloat);
        INTEGER = new StandardValueType<>(Integer.class, "int", Integer::parseInt);
        LONG = new StandardValueType<>(Long.class, "long", Long::parseLong);
        SHORT = new StandardValueType<>(Short.class, "short", Short::parseShort);
        STRING = new StandardValueType<>(String.class, "String", Function.identity());
        UUID = new StandardValueType<>(UUID.class, "UUID", java.util.UUID::fromString);
        VOID = new StandardValueType<>(void.class, "Void", it -> null);
        OBJECT = new StandardValueType<>(Object.class, "Object", it -> it);
        ARRAY = new StandardValueType<>(Object[].class, "Array", it -> new Object[]{it});

        values = new ValueType[]{BYTE, CHARACTER, DOUBLE, FLOAT, INTEGER, LONG, SHORT, STRING, BOOLEAN, VOID, ARRAY, OBJECT};
    }

    private final Class<R> type;
    private final String name;
    private final Function<String, R> converter;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<R> getTargetClass() {
        return type;
    }

    public StandardValueType(Class<R> type, String name, Function<String, R> mapper) {
        this.type = type;
        this.name = name;
        this.converter = mapper;
    }

    @Experimental
    public static Object findGoodType(String parse) {
        if (parse == null || parse.equals("null"))
            return null;
        if (parse.matches("\\d{1,9}"))
            return Integer.parseInt(parse);
        if (parse.matches("\\d{10,}"))
            return Long.parseLong(parse);
        if (parse.matches("\\d+[,.]\\d+"))
            return Double.parseDouble(parse);
        if (parse.matches("(true)|(false)"))
            return Boolean.parseBoolean(parse);
        return parse;
    }

    public static <T> ValueType<T> typeOf(T value) {
        if (value == null)
            //noinspection unchecked
            return (ValueType<T>) StandardValueType.VOID;
        return Arrays.stream(values)
                .filter(it -> it.test(value))
                .findAny()
                .map(Polyfill::<StandardValueType<T>>uncheckedCast)
                .orElse(null);
    }

    public static Rewrapper<ValueType<?>> forClass(Class<?> cls) {
        return Rewrapper.ofOptional(Arrays.stream(values)
                .filter(it -> it.getTargetClass().isAssignableFrom(cls) || (cls.isPrimitive() && it.getName().equals(cls.getSimpleName())))
                .findAny());
    }

    @Override
    public R parse(String data) {
        return converter.apply(data);
    }
}
