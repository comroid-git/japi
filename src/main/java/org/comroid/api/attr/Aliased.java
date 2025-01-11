package org.comroid.api.attr;

import org.comroid.annotations.AnnotatedTarget;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.func.util.Streams;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.util.stream.Stream;

public interface Aliased extends AnnotatedTarget.Extension {
    static Stream<String> $(AnnotatedElement element) {
        return Stream.concat(Stream.of(element).flatMap(Streams.cast(Named.class)).flatMap(n -> Stream.of(n.getPrimaryName(), n.getAlternateName())),
                Annotations.aliases(element).stream());
    }

    default String name() {
        return Stream.concat($names(), aliases()).findAny().orElseGet(this::toString);
    }

    default Stream<String> names() {
        return Stream.concat($names(), aliases());
    }

    default Stream<String> aliases() {
        return element().stream().flatMap(Aliased::$);
    }

    private @NotNull Stream<String> $names() {
        return Stream.of(this).flatMap(Streams.cast(Named.class)).map(Named::getBestName);
    }
}
