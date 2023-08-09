package org.comroid.api;

import org.comroid.util.Markdown;
import org.comroid.util.Pair;
import org.intellij.lang.annotations.Language;

import java.lang.annotation.*;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface TextDecoration extends StringAttribute, UnaryOperator<CharSequence>, Predicate<CharSequence>, WrappedFormattable {
    CharSequence getPrefix();

    CharSequence getSuffix();

    @Override
    default CharSequence apply(CharSequence seq) {
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
