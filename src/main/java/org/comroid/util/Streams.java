package org.comroid.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@UtilityClass
public class Streams {
    public static <T> Stream<T> of(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static <T> Stream<T> of(Spliterator<T> spliterator) {
        return StreamSupport.stream(spliterator, false);
    }

    @SafeVarargs
    public static <I> Collector<I, Collection<I>, Stream<I>> append(I... values) {
        return append(Arrays.asList(values));
    }

    public static <I> Collector<I, Collection<I>, Stream<I>> append(Iterable<I> values) {
        return append(of(values));
    }

    public static <I> Collector<I, Collection<I>, Stream<I>> append(Stream<I> values) {
        return Collector.of(ArrayList::new, Collection::add, (l, r) -> {
            l.addAll(r);
            return l;
        }, is -> Stream.concat(is.stream(), values));
    }

    public static <I> Function<I, Stream<I>> yield(final Predicate<I> filter, final Consumer<I> elseConsumer) {
        return obj -> {
            if (filter.test(obj))
                return Stream.of(obj);
            elseConsumer.accept(obj);
            return Stream.empty();
        };
    }

    public static <I, O> Function<I, Stream<O>> cast(final Class<O> type) {
        return obj -> Stream.of(obj).filter(type::isInstance).map(type::cast);
    }
}
