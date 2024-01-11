package org.comroid.api.data.seri;

import lombok.Getter;
import lombok.Value;
import org.comroid.api.Polyfill;
import org.comroid.api.func.ext.Wrap;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus.Experimental;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;

@Value
public class StandardValueType<R> implements ValueType<R> {
    public static final StandardValueType<Boolean> BOOLEAN;
    public static final StandardValueType<Byte> BYTE;
    public static final StandardValueType<Character> CHARACTER;
    public static final StandardValueType<Double> DOUBLE;
    public static final StandardValueType<Float> FLOAT;
    public static final StandardValueType<Integer> INTEGER;
    public static final StandardValueType<Long> LONG;
    public static final StandardValueType<Short> SHORT;
    public static final StandardValueType<String> STRING;
    public static final StandardValueType<UUID> UUID;

    public static final StandardValueType<Void> VOID;
    @Deprecated
    public static final StandardValueType<Object> OBJECT;
    public static final StandardValueType<Object[]> ARRAY;

    public static final StandardValueType<?>[] values;

    static {
        BOOLEAN = new StandardValueType<>(Boolean.class, "boolean", Boolean::parseBoolean, "checkbox");
        BYTE = new StandardValueType<>(Byte.class, "byte", Byte::parseByte, "number", "min='"+Byte.MIN_VALUE+"'", "max='"+Byte.MAX_VALUE+"'");
        CHARACTER = new StandardValueType<>(Character.class, "char", str -> str.toCharArray()[0], "text", "pattern='\\w'");
        DOUBLE = new StandardValueType<>(Double.class, "double", Double::parseDouble, "number", "min='"+Double.MIN_VALUE+"'", "max='"+Double.MAX_VALUE+"'");
        FLOAT = new StandardValueType<>(Float.class, "float", Float::parseFloat, "number", "min='"+Float.MIN_VALUE+"'", "max='"+Float.MAX_VALUE+"'");
        INTEGER = new StandardValueType<>(Integer.class, "int", Integer::parseInt, "number", "min='"+Integer.MIN_VALUE+"'", "max='"+Integer.MAX_VALUE+"'");
        LONG = new StandardValueType<>(Long.class, "long", Long::parseLong, "number", "min='"+Long.MIN_VALUE+"'", "max='"+Long.MAX_VALUE+"'");
        SHORT = new StandardValueType<>(Short.class, "short", Short::parseShort, "number", "min='"+Short.MIN_VALUE+"'", "max='"+Short.MAX_VALUE+"'");
        STRING = new StandardValueType<>(String.class, "String", Function.identity(), "text");
        UUID = new StandardValueType<>(UUID.class, "UUID", java.util.UUID::fromString, "text", "pattern='"+RegExpUtil.UUID4_PATTERN+"'");
        VOID = new StandardValueType<>(void.class, "Void", it -> null, "hidden");
        OBJECT = new StandardValueType<>(Object.class, "Object", it -> it, "hidden");
        ARRAY = new StandardValueType<>(Object[].class, "Array", it -> new Object[]{it}, "hidden");

        values = new StandardValueType[]{BYTE, CHARACTER, DOUBLE, FLOAT, INTEGER, LONG, SHORT, STRING, BOOLEAN, VOID, ARRAY};
    }

    private StandardValueType(
            Class<R> targetClass,
            String name,
            Function<String, R> converter,
            @Language(value = "HTML", prefix = "<input type=\"", suffix = "\">") String htmlInputType,
            @Language(value = "HTML", prefix = "<input ", suffix = ">") String... htmlInputAttributes) {
        this.targetClass = targetClass;
        this.name = name;
        this.converter = converter;
        this.htmlInputType = htmlInputType;
        this.htmlInputAttributes = htmlInputAttributes;
    }

    Class<R> targetClass;
    String name;
    Function<String, R> converter;
    @Language(value = "HTML", prefix = "<input type=\"", suffix = "\">")
    String htmlInputType;
    @Language(value = "HTML", prefix = "<input ", suffix = ">")
    String[] htmlInputAttributes;

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

    public static Wrap<ValueType<?>> forClass(Class<?> cls) {
        return Wrap.ofOptional(Arrays.stream(values)
                .filter(it -> it.getTargetClass().isAssignableFrom(cls) || (cls.isPrimitive() && it.getName().equals(cls.getSimpleName())))
                .findAny());
    }

    @Override
    public R parse(String data) {
        return converter.apply(data);
    }
}
