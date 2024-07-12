package org.comroid.api.attr;

import org.comroid.annotations.AnnotatedTarget;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.func.util.Streams;

import java.lang.reflect.AnnotatedElement;
import java.util.stream.Stream;

public interface Aliased extends AnnotatedTarget.Extension {
    default Stream<String> names() {
        return element().stream().flatMap(Aliased::$);
    }

    default String name() {
        return Stream.concat(names(), Stream.of(this)
                        .flatMap(Streams.cast(Named.class))
                        .map(Named::getBestName))
                .findAny()
                .orElseGet(this::toString);
    }

    static Stream<String> $(AnnotatedElement element) {
        return Stream.concat(
                Stream.of(element)
                        .flatMap(Streams.cast(Named.class))
                        .flatMap(n -> Stream.of(n.getPrimaryName(), n.getAlternateName())),
                Annotations.aliases(element).stream());
    }
}
