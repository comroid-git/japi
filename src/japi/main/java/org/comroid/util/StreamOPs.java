package org.comroid.util;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class StreamOPs {
    private StreamOPs() {
        throw new UnsupportedOperationException();
    }

    public static <T, R, IC extends Collection<T>> List<R> map(
            IC values,
            Function<T, R> mapper
    ) {
        return map(values, mapper, Collectors.toList());
    }

    public static <T, R, IC extends Collection<T>, RC extends Collection<R>> RC map(
            IC values,
            Function<T, R> mapper,
            Collector<R, ?, RC> collector
    ) {
        return values.stream()
                .map(mapper)
                .collect(collector);
    }
}
