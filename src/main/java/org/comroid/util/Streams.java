package org.comroid.util;

import lombok.Value;
import lombok.experimental.UtilityClass;
import org.comroid.api.N;
import org.comroid.api.Named;
import org.comroid.api.Polyfill;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Stream.empty;

@UtilityClass
public class Streams {
    public static <T> Stream<T> of(Iterator<T> iterator, int size) {
        return of(Spliterators.spliterator(iterator, size, 0));
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

    public static String intString(IntStream intStream) {
        return intStream.collect(
                        () -> new AtomicReference<>(""),
                        (l, r) -> l.updateAndGet(s -> s + (char) r),
                        (l, r) -> l.updateAndGet(s -> s + r.get()))
                .get();
    }

    @UtilityClass
    public class Multi {
        //region main methods
        @WrapWith("map")
        public <A, B> Function<A, Entry<A, B>> explode(final @NotNull Function<A, B> function) {
            return t -> new SimpleImmutableEntry<>(t, function.apply(t));
        }

        @WrapWith("map")
        public <A, B, X> Function<Entry<A, B>, Entry<X, B>> crossToA(final @NotNull BiFunction<A, B, X> function) {
            return cross(function, (a, b) -> b);
        }

        @WrapWith("map")
        public <A, B, Y> Function<Entry<A, B>, Entry<A, Y>> crossToB(final @NotNull BiFunction<A, B, Y> function) {
            return cross((a, b) -> a, function);
        }

        @WrapWith("map")
        public <A, B, X, Y> Function<Entry<A, B>, Entry<X, Y>> cross(
                final @NotNull BiFunction<A, B, X> xFunction,
                final @NotNull BiFunction<A, B, Y> yFunction) {
            return e -> new SimpleImmutableEntry<>(
                    xFunction.apply(e.getKey(), e.getValue()),
                    yFunction.apply(e.getKey(), e.getValue()));
        }

        @WrapWith("map")
        public <A, B, R> Function<Entry<A, B>, R> combine(final @NotNull BiFunction<A, B, R> function) {
            return e -> function.apply(e.getKey(), e.getValue());
        }
        //endregion

        //region regular stream functions
        @WrapWith("peek")
        public <A, B> Consumer<Entry<A, B>> peekA(final @NotNull Consumer<A> consumer) {
            return peek(consumer, N.Consumer.nop());
        }

        @WrapWith("peek")
        public <A, B> Consumer<Entry<A, B>> peekB(final @NotNull Consumer<B> consumer) {
            return peek(N.Consumer.nop(), consumer);
        }

        @WrapWith("peek")
        public <A, B> Consumer<Entry<A, B>> peek(
                final @NotNull Consumer<A> aConsumer,
                final @NotNull Consumer<B> bConsumer) {
            return peek((a, b) -> {
                aConsumer.accept(a);
                bConsumer.accept(b);
            });
        }

        @WrapWith("peek")
        public <A, B> Consumer<Entry<A, B>> peek(final @NotNull BiConsumer<A, B> consumer) {
            return e -> consumer.accept(e.getKey(), e.getValue());
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filterA(@NotNull Predicate<A> predicate) {
            return filter(predicate, $ -> true);
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filterB(@NotNull Predicate<B> predicate) {
            return filter($ -> true, predicate);
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filter(
                @NotNull Predicate<A> aPredicate,
                @NotNull Predicate<B> bPredicate) {
            return filter(OP.LogicalAnd, aPredicate, bPredicate);
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filter(
                final @NotNull OP op,
                final @NotNull Predicate<A> aPredicate,
                final @NotNull Predicate<B> bPredicate) {
            return filter((a, b) -> op.test(() -> aPredicate.test(a), () -> bPredicate.test(b)));
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filter(final @NotNull BiPredicate<A, B> predicate) {
            return e -> predicate.test(e.getKey(), e.getValue()) ? Stream.of(e) : empty();
        }

        @WrapWith("map")
        public <A, B, X> Function<Entry<A, B>, Entry<X, B>> mapA(final @NotNull Function<A, X> function) {
            return map(function, Function.identity());
        }

        @WrapWith("map")
        public <A, B, Y> Function<Entry<A, B>, Entry<A, Y>> mapB(final @NotNull Function<B, Y> function) {
            return map(Function.identity(), function);
        }

        @WrapWith("map")
        public <A, B, X, Y> Function<Entry<A, B>, Entry<X, Y>> map(
                final @NotNull Function<A, X> axFunction,
                final @NotNull Function<B, Y> byFunction) {
            return e -> new SimpleImmutableEntry<>(axFunction.apply(e.getKey()), byFunction.apply(e.getValue()));
        }

        @WrapWith("flatMap")
        public <A, B, X> Function<Entry<A, B>, Stream<Entry<X, B>>> flatMapA(final @NotNull Function<A, Stream<X>> function) {
            return flatMap(Adapter.sideA(), function);
        }

        @WrapWith("flatMap")
        public <A, B, Y> Function<Entry<A, B>, Stream<Entry<A, Y>>> flatMapB(final @NotNull Function<B, Stream<Y>> function) {
            return flatMap(Adapter.sideB(), function);
        }

        @WrapWith("flatMap")
        public <A, B, X, Y, I, O> Function<Entry<A, B>, Stream<Entry<X, Y>>> flatMap(
                final @NotNull Adapter<A, B, X, Y, I, O> adapter,
                final @NotNull Function<I, Stream<O>> function) {
            return e -> function.apply(adapter.input.apply(e)).map(o -> adapter.merge(e, o));
        }

        @WrapWith("forEach")
        public <A, B> Consumer<Entry<A, B>> forEach(
                final @NotNull Consumer<A> aConsumer,
                final @NotNull Consumer<B> bConsumer) {
            return forEach((a, b) -> {
                aConsumer.accept(a);
                bConsumer.accept(b);
            });
        }

        @WrapWith("forEach")
        public <A, B> Consumer<Entry<A, B>> forEach(final @NotNull BiConsumer<A, B> consumer) {
            return e -> consumer.accept(e.getKey(), e.getValue());
        }

        @WrapWith("collect")
        public <K, V> Collector<Entry<K, V>, Map<K, V>, Map<K, V>> collector() {
            return collector(HashMap::new);
        }

        @WrapWith("collect")
        public <K, V> Collector<Entry<K, V>, Map<K, V>, Map<K, V>> collector(final Supplier<Map<K, V>> mapSupplier) {
            return Collector.of(mapSupplier,
                    (m, e) -> m.put(e.getKey(), e.getValue()),
                    (a, b) -> {
                        a.putAll(b);
                        return a;
                    },
                    Collections::unmodifiableMap);
        }
        //endregion

        @Value
        public static class Adapter<A, B, X, Y, I, O> {
            Function<Entry<A, B>, I> input;
            BiFunction<Entry<A, B>, O, X> outputX;
            BiFunction<Entry<A, B>, O, Y> outputY;

            public static <A, B, X> Adapter<A, B, X, B, A, X> sideA() {
                return new Adapter<>(Entry::getKey, (e, x) -> x, (e, y) -> e.getValue());
            }

            public static <A, B, Y> Adapter<A, B, A, Y, B, Y> sideB() {
                return new Adapter<>(Entry::getValue, (e, y) -> e.getKey(), (e, y) -> y);
            }

            private Entry<X, Y> merge(Entry<A, B> entry, O output) {
                return new SimpleImmutableEntry<>(outputX.apply(entry, output), outputY.apply(entry, output));
            }
        }

        @Retention(RetentionPolicy.CLASS)
        private @interface WrapWith {
            String value();
        }
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

    public enum OP implements BiPredicate<BooleanSupplier, BooleanSupplier>, Named {
        LogicalAnd {
            @Override
            public boolean test(BooleanSupplier l, BooleanSupplier r) {
                return l.getAsBoolean() && r.getAsBoolean();
            }
        },
        LogicalOr {
            @Override
            public boolean test(BooleanSupplier l, BooleanSupplier r) {
                return l.getAsBoolean() || r.getAsBoolean();
            }
        },
        LogicalXor {
            @Override
            public boolean test(BooleanSupplier l, BooleanSupplier r) {
                return l.getAsBoolean() ^ r.getAsBoolean();
            }
        },
        BitwiseAnd {
            @Override
            public boolean test(BooleanSupplier l, BooleanSupplier r) {
                return l.getAsBoolean() & r.getAsBoolean();
            }
        },
        BitwiseOr {
            @Override
            public boolean test(BooleanSupplier l, BooleanSupplier r) {
                return l.getAsBoolean() | r.getAsBoolean();
            }
        };

        @Override
        public abstract boolean test(BooleanSupplier l, BooleanSupplier r);
    }
}
