package org.comroid.api;

import org.comroid.util.Markdown;
import org.comroid.util.Pair;
import org.comroid.util.Streams;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.*;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface TextDecoration extends StringAttribute, Function<CharSequence, String>, Predicate<CharSequence>, WrappedFormattable {
    CharSequence getPrefix();

    CharSequence getSuffix();

    @Override
    default String apply(CharSequence seq) {
        return String.valueOf(getPrefix()) + seq + getSuffix();
    }

    @Override
    default boolean test(CharSequence seq) {
        final var str = seq.toString();
        var i = str.indexOf(getPrefix().toString());
        return i != -1 && str.indexOf(getSuffix().toString(), i) != -1;
    }

    @Override
    default String getString() {
        return getPrefix().toString();
    }

    @Contract("null, _ -> null; !null, _ -> !null")
    static <SRC extends TextDecoration> String sanitize(
            @Nullable CharSequence seq,
            Class<SRC> of
    ) {
        return sanitize(seq, of, null);
    }

    @Contract("null, _, _ -> null; !null, _, _ -> !null")
    static <SRC extends TextDecoration, TGT extends TextDecoration> String sanitize(
            @Nullable CharSequence seq,
            Class<SRC> of,
            @Nullable Class<TGT> output
    ) {
        if (seq == null) return null;
        var str = convert(seq, of, output);
        for (SRC decorator : Arrays.stream(of.getFields())
                .filter(fld -> Modifier.isStatic(fld.getModifiers()))
                .map(ThrowingFunction.rethrowing(fld -> fld.get(null)))
                .flatMap(Streams.cast(of))
                .toList())
            str = str.replace(decorator.getPrefix(), "").replace(decorator.getSuffix(), "");
        return str;
    }

    static <SRC extends TextDecoration, TGT extends TextDecoration> String convert(
            CharSequence seq,
            Class<SRC> from,
            Class<TGT> to
    ) {
        var str = seq.toString();
        for (var pair : replacers(from, to).toList())
            str = str.replaceAll(pair.getFirst(), pair.getSecond());
        return str;
    }

    static <SRC extends TextDecoration, TGT extends TextDecoration> Stream<Pair<String, String>> replacers(
            Class<SRC> from,
            Class<TGT> to
    ) {
        @Language("RegExp")
        final var patternBase = "(%s)([\\w\\s]+)[^\\\\]??(%s)?"; // todo: escape sequences are broken
        return styles(from, to).entrySet().stream()
                .map(e -> new Pair<>(
                        patternBase.formatted(e.getKey().getPrefix(), e.getKey().getSuffix()).replace("*", "\\*"),
                        "%s$2%s".formatted(e.getValue().getPrefix(), e.getValue().getSuffix())
                ));
    }

    static <SRC extends TextDecoration, TGT extends TextDecoration> Map<SRC, TGT> styles(
            final Class<SRC> from,
            final Class<TGT> to
    ) {
        return Stream.of(Italic.class, Bold.class, Underline.class, Strikethrough.class, Verbatim.class)
                .flatMap(ann -> findField(from, ann)
                        .flatMap(in -> findField(to, ann)
                                .map(out -> new Pair<>(in, out)))
                        .stream())
                .collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond));
    }
    
    private static <T> Optional<T> findField(Class<T> type, Class<? extends Annotation> annotated) {
        return Arrays.stream(type.getFields())
                .filter(fld -> type.isAssignableFrom(fld.getType()))
                .filter(fld -> fld.isAnnotationPresent(annotated)
                        || fld.getName().equalsIgnoreCase(annotated.getName()))
                .filter(fld -> Modifier.isStatic(fld.getModifiers()))
                .map(ThrowingFunction.rethrowing(fld -> fld.get(null)))
                .map(type::cast)
                .findAny();
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Italic {
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Bold {
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Underline {
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Strikethrough {
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Verbatim {
    }
}
