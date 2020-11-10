package org.comroid.api;

import java.util.function.Function;

public interface HeldType<R> extends Named {
    @Deprecated
    default Function<String, R> getConverter() {
        return this::parse;
    }

    R parse(String data);

    <T> T convert(R value, HeldType<T> toType);
}
