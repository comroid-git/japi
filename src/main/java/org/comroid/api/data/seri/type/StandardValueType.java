package org.comroid.api.data.seri.type;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.comroid.api.Polyfill;
import org.comroid.api.data.RegExpUtil;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.html.form.HtmlInputDesc;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

@Value
@ToString(of = "name")
@EqualsAndHashCode(of = "targetClass")
public class StandardValueType<R> implements ValueType<R>, HtmlInputDesc {
    private static final Set<StandardValueType<?>> $cache = new HashSet<>();
    public static final Set<StandardValueType<?>> cache = Collections.unmodifiableSet($cache);
    public static final StandardValueType<Boolean> BOOLEAN = new StandardValueType<>(Boolean.class, boolean.class, "boolean", Boolean::parseBoolean, "checkbox");
    public static final StandardValueType<Byte> BYTE = new StandardValueType<>(Byte.class, byte.class, "byte", Byte::parseByte, "number", "min='"+Byte.MIN_VALUE+"'", "max='"+Byte.MAX_VALUE+"'");
    public static final StandardValueType<Character> CHARACTER = new StandardValueType<>(Character.class, char.class, "char", str -> str.toCharArray()[0], "text", "pattern='\\w'");
    public static final StandardValueType<Double> DOUBLE = new StandardValueType<>(Double.class, double.class, "double", Double::parseDouble, "number", "min='"+Double.MIN_VALUE+"'", "max='"+Double.MAX_VALUE+"'");
    public static final StandardValueType<Float> FLOAT = new StandardValueType<>(Float.class, float.class, "float", Float::parseFloat, "number", "min='"+Float.MIN_VALUE+"'", "max='"+Float.MAX_VALUE+"'");
    public static final StandardValueType<Integer> INTEGER = new StandardValueType<>(Integer.class, int.class, "int", Integer::parseInt, "number", "min='"+Integer.MIN_VALUE+"'", "max='"+Integer.MAX_VALUE+"'");
    public static final StandardValueType<Long> LONG = new StandardValueType<>(Long.class, long.class, "long", Long::parseLong, "number", "min='"+Long.MIN_VALUE+"'", "max='"+Long.MAX_VALUE+"'");
    public static final StandardValueType<Short> SHORT = new StandardValueType<>(Short.class, short.class, "short", Short::parseShort, "number", "min='"+Short.MIN_VALUE+"'", "max='"+Short.MAX_VALUE+"'");
    public static final StandardValueType<String> STRING = new StandardValueType<>(String.class, "String", Function.identity(), "text");
    public static final StandardValueType<UUID> UUID = new StandardValueType<>(UUID.class, "UUID", java.util.UUID::fromString, "text", "pattern='"+ RegExpUtil.UUID4_PATTERN+"'");

    public static final StandardValueType<Void> VOID = new StandardValueType<>(void.class, "Void", it -> null, "hidden");
    /**
     * @deprecated use {@link BoundValueType}
     */
    @Deprecated(forRemoval = true)
    public static final StandardValueType<Object> OBJECT = new StandardValueType<>(Object.class, "Object", it -> it, "hidden");
    /**
     * @deprecated use {@link ArrayValueType}
     */
    @Deprecated(forRemoval = true)
    public static final StandardValueType<Object[]> ARRAY = new StandardValueType<>(Object[].class, "Array", it -> new Object[]{it}, "hidden");

    @lombok.Builder
    private StandardValueType(
            Class<R> targetClass,
            String name,
            Function<String, R> converter,
            @Language(value = "HTML", prefix = "<input type=\"", suffix = "\">") String htmlInputType,
            @Language(value = "HTML", prefix = "<input ", suffix = ">") String... htmlInputAttributes) {
        this(targetClass, null, name, converter, htmlInputType, htmlInputAttributes);
    }

    private StandardValueType(
            Class<R> targetClass,
            @Nullable Class<?> primitiveClass,
            String name,
            Function<String, R> converter,
            @Language(value = "HTML", prefix = "<input type=\"", suffix = "\">") String htmlInputType,
            @Language(value = "HTML", prefix = "<input ", suffix = ">") String... htmlInputAttributes) {
        this.targetClass = targetClass;
        this.primitiveClass = primitiveClass;
        this.name = name;
        this.converter = converter;
        this.htmlInputType = htmlInputType;
        this.htmlInputAttributes = htmlInputAttributes;

        if (!List.of("Object", "Array").contains(name))
            $cache.add(this);
    }

    Class<R> targetClass;
    @Nullable Class<?> primitiveClass;
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
        if (parse.matches(RegExpUtil.UUID4_PATTERN))
            return java.util.UUID.fromString(parse);
        return parse;
    }

    public static <T> ValueType<T> typeOf(T value) {
        if (value == null)
            //noinspection unchecked
            return (ValueType<T>) StandardValueType.VOID;
        return cache.stream()
                .filter(it -> it.test(value))
                .findAny()
                .map(Polyfill::<StandardValueType<T>>uncheckedCast)
                .orElse(null);
    }

    public static Wrap<ValueType<?>> forClass(Class<?> cls) {
        return Wrap.of(cache.stream()
                .filter(it -> it.getTargetClass().isAssignableFrom(cls) || (cls.isPrimitive() && it.getName().equals(cls.getSimpleName())))
                .findAny());
    }

    @Override
    public R parse(String data) {
        return converter.apply(data);
    }
}
