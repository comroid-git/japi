package org.comroid.util;

import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.empty;
import static java.util.Optional.of;

@UtilityClass
public class Optionals {
    public static <I, O> Function<I, Optional<O>> cast(Class<? extends O> type) {
        return x -> type.isInstance(x) ? of(type.cast(x)) : empty();
    }
}
