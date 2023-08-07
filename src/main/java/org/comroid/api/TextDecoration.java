package org.comroid.api;

import org.comroid.util.Pair;
import org.intellij.lang.annotations.Language;

import java.lang.annotation.*;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface TextDecoration extends UnaryOperator<CharSequence>, Predicate<CharSequence>, WrappedFormattable {
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

    static <SRC extends TextDecoration, TGT extends TextDecoration> String convert(
            CharSequence seq,
            Class<SRC> from,
            Class<TGT> to
    ) {
        final var regexEscape = new char[]{'*'};
        @Language("RegExp")
        final var patternBase = "%s([\\w\\s]+)[^\\\\]??(%s)?"; // todo: escape sequences are broken
        var str = seq.toString();
        final var styles = styles(from, to);
        for (var entry : styles.entrySet()) {
            var pattern = patternBase.formatted(entry.getKey().getPrefix(), entry.getKey().getSuffix());
            var replace = "%s$1%s".formatted(entry.getValue().getPrefix(), entry.getValue().getSuffix());
            for (char x : regexEscape)
                pattern = pattern.replace(String.valueOf(x), "\\"+x);
            while (entry.getKey().test(str))
                str = str.replaceAll(pattern, replace);
        }
        return str;
    }

    private static <SRC extends TextDecoration, TGT extends TextDecoration> Map<SRC, TGT> styles(
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
