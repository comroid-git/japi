package org.comroid.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Stream.empty;

@UtilityClass
public class Streams {
    public static <T> Stream<T> of(Iterator<T> iterator,int size) {
        return of(Spliterators.spliterator(iterator,size,0));
    }

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

    public static <I> Function<I, Stream<I>> yield(final int next, final Consumer<I> elseConsumer) {
        return new Function<>() {
            final AtomicInteger count = new AtomicInteger(0);

            @Override
            public Stream<I> apply(I obj) {
                if (count.incrementAndGet() >= next)
                    return Stream.of(obj);
                elseConsumer.accept(obj);
                return empty();
            }
        };
    }

    public static <I> Function<I, Stream<I>> yield(final Predicate<I> filter, final Consumer<I> elseConsumer) {
        return obj -> {
            if (filter.test(obj))
                return Stream.of(obj);
            elseConsumer.accept(obj);
            return empty();
        };
    }

    public static <I, O> Function<I, Stream<O>> cast(final Class<O> type) {
        return obj -> Stream.of(obj).filter(type::isInstance).map(type::cast);
    }

    public static <T> Collector<T, List<T>, Optional<T>> oneOrNone(final @Nullable Supplier<RuntimeException> exception) {
        return Collector.of(ArrayList::new, List::add, (l, r) -> {
            l.addAll(r);
            return l;
        }, ls -> {
            if (ls.size() > 1) {
                if (exception != null)
                    throw exception.get();
                else return Optional.empty();
            }
            return ls.isEmpty() ? Optional.empty() : Optional.of(ls.get(0));
        });
    }

    public enum Strategy {
        Every,
        Opposite,
        While,
        Until;

        public final class Filter<T> implements Predicate<T>, UnaryOperator<@NotNull Boolean> {
            private final Predicate<T> predicate;
            private boolean state = Strategy.this == While;

            public Filter(Predicate<T> predicate) {
                this.predicate = predicate;
            }

            public @NotNull Boolean peek(@NotNull Boolean testResult) {
                return switch (Strategy.this) {
                    case Every -> testResult;
                    case Opposite -> !testResult;
                    case While -> state && testResult;
                    case Until -> !state && !testResult;
                };
            }

            @Override
            public @NotNull Boolean apply(@NotNull Boolean testResult) {
                return (state = peek(testResult));
            }

            @Override
            public boolean test(T t) {
                return apply(predicate.test(t));
            }
        }
    }
}
