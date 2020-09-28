package org.comroid.api;

import java.util.function.Function;

public interface HeldType<R> extends Named {
    Function<String, R> getConverter();

    <T> T convert(R value, HeldType<T> toType);
}
