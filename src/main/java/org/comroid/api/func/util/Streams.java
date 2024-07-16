package org.comroid.api.func.util;

import lombok.Value;
import lombok.experimental.UtilityClass;
import org.comroid.api.attr.Named;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.*;
import static java.util.function.Function.*;
import static java.util.stream.Stream.*;
import static org.comroid.api.func.N.Consumer.*;

@UtilityClass
public class Streams {
    public static <T> Stream<T> of(Iterator<T> iterator, int size) {
        return of(Spliterators.spliterator(iterator, size, 0));
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

    public static <T> Stream<T> of(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static <I> Collector<I, Collection<I>, Stream<I>> append(Stream<? extends I> values) {
        return Collector.of(ArrayList::new, Collection::add, (l, r) -> {
            l.addAll(r);
            return l;
        }, is -> Stream.concat(is.stream(), values));
    }

    public static <I> Function<I, Stream<I>> filter(final int next, final Consumer<I> elseConsumer) {
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

    public static <I> Function<I, Stream<I>> filter(final Predicate<I> filter, final Consumer<I> elseConsumer) {
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

    /* todo
    private static <T> Collector<T, List<T>, Stream<List<T>>> batches(int size) {
        return Collector.of(
                ArrayList::new,
                List::add,
                (a,b)->{a.addAll(b);return b;},
                ls -> {
                    final var out = new ArrayList<T>();
                    for(var i = 0;i<ls.size();i++)
                        out.add(List.of())
                    return out.stream();
                });
    }
     */

    @SafeVarargs
    public static <T, R> Function<T, Stream<R>> multiply(Function<? super T, Stream<? extends R>>... function) {
        return t -> Stream.of(function).flatMap(func -> func.apply(t));
    }

    public static <T> Function<T, Stream<T>> expand(Function<? super T, Stream<? extends T>> by) {
        return expand(identity(), by);
    }

    public static <T, R> Function<T, Stream<R>> expand(Function<? super T, R> base, Function<? super T, Stream<? extends R>> by) {
        return it -> concat(Stream.of(it).map(base), by.apply(it));
    }

    @SuppressWarnings("ReplaceInefficientStreamCount")
    public static <T> Collector<T, Set<T>, Stream<T>> expandRecursive(Function<? super T, Stream<? extends T>> by) {
        return Collector.of(
                HashSet::new,
                Collection::add,
                (l, r) -> Stream.concat(l.stream(), r.stream())
                        .collect(Collectors.toSet()),
                out -> {
                    Set<T> ls = new HashSet<>(), buf1 = out, buf2 = ls;
                    while (buf1.stream()
                            .flatMap(by)
                            .filter(buf2::add)
                            .count() > 0) {
                        var buf0 = buf1;
                        buf1 = buf2;
                        buf2 = buf0;
                    }
                    return concat(buf1.stream(), buf2.stream()).distinct();
                });
    }

    public static <T> Collector<T, List<T>, List<List<T>>> groupingEvery(long groupSize) {
        return Collector.of(ArrayList::new, List::add, (l, r) -> {
            l.addAll(r);
            return l;
        }, ls -> {
            var out  = new ArrayList<List<T>>();
            var iter = ls.iterator();
            ArrayList<T> group = null;
            while (iter.hasNext()) {
                if (group == null)
                    group = new ArrayList<>();
                group.add(iter.next());
                if (group.size() >= groupSize) {
                    out.add(unmodifiableList(group));
                    group = null;
                }
            }
            return unmodifiableList(out);
        });
    }

    public static <T> Collector<T, List<T>, Stream<T>> atLeastOneOrElseGet(Supplier<T> otherwise) {
        return Collector.of(ArrayList::new, List::add, (l, r) -> {
            l.addAll(r);
            return l;
        }, ls -> ls.isEmpty() ? Stream.of(otherwise.get()) : ls.stream());
    }

    public static <T> Collector<T, List<T>, Stream<T>> atLeastOneOrElseFlatten(Supplier<Stream<T>> otherwise) {
        return Collector.of(ArrayList::new, List::add, (l, r) -> {
            l.addAll(r);
            return l;
        }, ls -> ls.isEmpty() ? otherwise.get() : ls.stream());
    }

    public static <T> Collector<T, List<T>, Stream<T>> or(Supplier<Stream<T>> supplier) {
        return Collector.of(ArrayList::new, Collection::add, (l, r) -> {
            l.addAll(r);
            return l;
        }, ls -> {
            if (ls.isEmpty())
                return supplier.get();
            return ls.stream();
        });
    }

    @Deprecated(forRemoval = true)
    public static <T, C> Comparator<T> comparatorAdapter(final @NotNull Function<T, C> mapper, final @NotNull Comparator<C> comparator) {
        return (a, b) -> comparator.compare(mapper.apply(a), mapper.apply(b));
    }

    public enum Strategy {
        Every,
        Opposite,
        While,
        Until;

        public final class Filter<T> implements Predicate<T>, UnaryOperator<@NotNull Boolean> {
            private final Predicate<T> predicate;
            private       boolean      state = Strategy.this == While;

            public Filter(Predicate<T> predicate) {
                this.predicate = predicate;
            }

            @Override
            public boolean test(T t) {
                return apply(predicate.test(t));
            }

            @Override
            public @NotNull Boolean apply(@NotNull Boolean testResult) {
                return (state = peek(testResult));
            }

            public @NotNull Boolean peek(@NotNull Boolean testResult) {
                return switch (Strategy.this) {
                    case Every -> testResult;
                    case Opposite -> !testResult;
                    case While -> state && testResult;
                    case Until -> !state && !testResult;
                };
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
        public final java.util.stream.Collector<@NotNull BooleanSupplier, @NotNull Boolean, @NotNull Boolean> Collector
                = java.util.stream.Collector.of(() -> false, (l, r) -> test(() -> l, r), (l, r) -> test(() -> l, () -> r));

        public boolean test(boolean l, boolean r) {
            return test(() -> l, () -> r);
        }

        @Override
        public abstract boolean test(BooleanSupplier l, BooleanSupplier r);
    }

    @UtilityClass
    // todo: the first one blocks the second one; Stream.Multi is broken
    public class Multi {
        //region main methods
        @WrapWith("map")
        public <A, B> Function<A, Entry<A, B>> expand(final @NotNull Function<A, B> function) {
            return t -> new SimpleImmutableEntry<>(t, function.apply(t));
        }

        @WrapWith("flatMap")
        public <A, B> Function<A, Stream<Entry<A, B>>> expandFlat(final @NotNull Function<A, Stream<B>> function) {
            return a -> function.apply(a).map(b -> new SimpleImmutableEntry<>(a, b));
        }

        @WrapWith("map")
        public <A, B> Function<Entry<A, B>, Entry<B, A>> invert() {
            return e -> new SimpleImmutableEntry<>(e.getValue(), e.getKey());
        }

        @WrapWith("flatMap")
        public <A, B, X> Function<Entry<A, B>, Stream<Entry<X, B>>> routeA(final @NotNull Function<Stream<Entry<A, B>>, Stream<X>> function) {
            return flatMap(Adapter.sideA(), (e, a) -> function.apply(Stream.of(e)));
        }

        @WrapWith("flatMap")
        public <A, B, X, Y, I, O> Function<Entry<A, B>, Stream<Entry<X, Y>>> flatMap(
                final @NotNull Adapter<A, B, X, Y, I, O> adapter, final @NotNull BiFunction<Entry<A, B>, I, Stream<O>> function
        ) {
            return e -> function.apply(e, adapter.input.apply(e)).map(o -> adapter.merge(e, o));
        }

        @WrapWith("flatMap")
        public <A, B, Y> Function<Entry<A, B>, Stream<Entry<A, Y>>> routeB(final @NotNull Function<Stream<Entry<A, B>>, Stream<Y>> function) {
            return flatMap(Adapter.sideB(), (e, b) -> function.apply(Stream.of(e)));
        }

        @WrapWith("map")
        public <A, B, Y> Function<Entry<A, B>, Entry<A, Y>> crossA2B(final @NotNull Function<A, Y> function) {
            return crossA2B((a, $) -> function.apply(a));
        }

        @WrapWith("map")
        public <A, B, Y> Function<Entry<A, B>, Entry<A, Y>> crossA2B(final @NotNull BiFunction<A, B, Y> function) {
            return cross((a, b) -> a, function);
        }

        @WrapWith("map")
        public <A, B, X, Y> Function<Entry<A, B>, Entry<X, Y>> cross(
                final @NotNull BiFunction<A, B, X> xFunction, final @NotNull BiFunction<A, B, Y> yFunction
        ) {
            return e -> new SimpleImmutableEntry<>(xFunction.apply(e.getKey(), e.getValue()), yFunction.apply(e.getKey(), e.getValue()));
        }

        @WrapWith("map")
        public <A, B, X> Function<Entry<A, B>, Entry<X, B>> crossB2A(final @NotNull Function<B, X> function) {
            return crossB2A(($, b) -> function.apply(b));
        }

        @WrapWith("map")
        public <A, B, X> Function<Entry<A, B>, Entry<X, B>> crossB2A(final @NotNull BiFunction<A, B, X> function) {
            return cross(function, (a, b) -> b);
        }

        @WrapWith("flatMap")
        public <A, B, R> Function<Entry<A, B>, Stream<R>> merge(final @NotNull BiFunction<A, B, Stream<R>> function) {
            return e -> function.apply(e.getKey(), e.getValue());
        }
        //endregion

        @WrapWith("map")
        public <A, B, R> Function<Entry<A, B>, R> combine(final @NotNull BiFunction<A, B, R> function) {
            return e -> function.apply(e.getKey(), e.getValue());
        }

        //region peek
        @WrapWith("peek")
        public <T> Consumer<Entry<? extends T, ? extends T>> peekMono(final @NotNull Consumer<T> consumer) {
            return peek(consumer, consumer);
        }

        @WrapWith("peek")
        public <A, B> Consumer<Entry<? extends A, ? extends B>> peek(final @NotNull Consumer<A> aConsumer, final @NotNull Consumer<B> bConsumer) {
            return peek((a, b) -> {
                aConsumer.accept(a);
                bConsumer.accept(b);
            });
        }

        @WrapWith("peek")
        public <A, B> Consumer<Entry<? extends A, ? extends B>> peek(final @NotNull BiConsumer<A, B> consumer) {
            return e -> consumer.accept(e.getKey(), e.getValue());
        }

        @WrapWith("peek")
        public <A, B> Consumer<Entry<? extends A, ? extends B>> peekA(final @NotNull Consumer<A> consumer) {
            return peek(consumer, nop());
        }

        @WrapWith("peek")
        public <A, B> Consumer<Entry<? extends A, ? extends B>> peekB(final @NotNull Consumer<B> consumer) {
            return peek(nop(), consumer);
        }

        //endregion
        //region filter
        @WrapWith("flatMap")
        public <T> Function<Entry<T, T>, Stream<Entry<T, T>>> filterMono(@NotNull Predicate<T> predicate) {
            return filter(OP.LogicalAnd, predicate, predicate);
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filter(
                final @NotNull OP op, final @NotNull Predicate<A> aPredicate, final @NotNull Predicate<B> bPredicate
        ) {
            return filter((a, b) -> op.test(() -> aPredicate.test(a), () -> bPredicate.test(b)));
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filter(final @NotNull BiPredicate<A, B> predicate) {
            return filter(predicate, nop());
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filter(final @NotNull BiPredicate<A, B> predicate, final @NotNull BiConsumer<A, B> disposal) {
            return e -> {
                if (predicate.test(e.getKey(), e.getValue())) return Stream.of(e);
                disposal.accept(e.getKey(), e.getValue());
                return empty();
            };
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filterA(@NotNull Predicate<A> predicate) {
            return filter(predicate, $ -> true);
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filter(@NotNull Predicate<A> aPredicate, @NotNull Predicate<B> bPredicate) {
            return filter(OP.LogicalAnd, aPredicate, bPredicate);
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filterB(@NotNull Predicate<B> predicate) {
            return filter($ -> true, predicate);
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filterA(final @NotNull Predicate<A> predicate, final @NotNull Consumer<A> disposal) {
            return filter(predicate, disposal, $ -> true, nop());
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filter(
                final @NotNull Predicate<A> aPredicate, final @NotNull Consumer<A> aDisposal, final @NotNull Predicate<B> bPredicate,
                final @NotNull Consumer<B> bDisposal
        ) {
            return filter(OP.LogicalAnd, aPredicate, aDisposal, bPredicate, bDisposal);
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filter(
                final @NotNull OP op, final @NotNull Predicate<A> aPredicate, final @NotNull Consumer<A> aDisposal, final @NotNull Predicate<B> bPredicate,
                final @NotNull Consumer<B> bDisposal
        ) {
            return filter((a, b) -> op.test(() -> aPredicate.test(a), () -> bPredicate.test(b)), (a, b) -> {
                aDisposal.accept(a);
                bDisposal.accept(b);
            });
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filterB(final @NotNull Predicate<B> predicate, final @NotNull Consumer<B> disposal) {
            return filter($ -> true, nop(), predicate, disposal);
        }

        //endregion
        //region map
        @WrapWith("map")
        public <I, O> Function<Entry<I, I>, Entry<O, O>> mapMono(final @NotNull Function<I, O> function) {
            return map(function, function);
        }

        @WrapWith("map")
        public <A, B, X, Y> Function<Entry<A, B>, Entry<X, Y>> map(final @NotNull Function<A, X> axFunction, final @NotNull Function<B, Y> byFunction) {
            return e -> new SimpleImmutableEntry<>(axFunction.apply(e.getKey()), byFunction.apply(e.getValue()));
        }

        @WrapWith("map")
        public <A, B, X> Function<Entry<A, B>, Entry<X, B>> mapA(final @NotNull Function<A, X> function) {
            return map(function, identity());
        }

        @WrapWith("map")
        public <A, B, Y> Function<Entry<A, B>, Entry<A, Y>> mapB(final @NotNull Function<B, Y> function) {
            return map(identity(), function);
        }

        //endregion
        //region flatMap
        @WrapWith("flatMap")
        public <A, B, X> Function<Entry<A, B>, Stream<Entry<X, B>>> flatMapA(final @NotNull Function<A, Stream<X>> function) {
            return flatMapA((e, a) -> function.apply(a));
        }

        @WrapWith("flatMap")
        public <A, B, X> Function<Entry<A, B>, Stream<Entry<X, B>>> flatMapA(final @NotNull BiFunction<Entry<A, B>, A, Stream<X>> function) {
            return flatMap(Adapter.sideA(), function);
        }

        @WrapWith("flatMap")
        public <A, B, Y> Function<Entry<A, B>, Stream<Entry<A, Y>>> flatMapB(final @NotNull Function<B, Stream<Y>> function) {
            return flatMapB((e, b) -> function.apply(b));
        }

        @WrapWith("flatMap")
        public <A, B, Y> Function<Entry<A, B>, Stream<Entry<A, Y>>> flatMapB(final @NotNull BiFunction<Entry<A, B>, B, Stream<Y>> function) {
            return flatMap(Adapter.sideB(), function);
        }

        @WrapWith("flatMap")
        public <A, B, X, Y> Function<Entry<A, B>, Stream<Entry<X, Y>>> flatMap(final @NotNull Function<Stream<Entry<A, B>>, Stream<Entry<X, Y>>> function) {
            return flatMap(Adapter.tunnel(), ($, e) -> function.apply(Stream.of(e)));
        }

        /*
        todo implement batches()
        @WrapWith("flatMap")
        public <A, B, X, Y> Function<Entry<A, B>, Stream<Entry<X, Y>>> flatMap(
                final @NotNull BiFunction<A,B, Stream<X>> aFunction,
                final @NotNull BiFunction<A,B, Stream<Y>> bFunction
        ) {
            return e -> {
                return aFunction.apply(e.getKey(),e.getValue())
                        .map(x->new SimpleImmutableEntry<>(x,null))
                        .collect(append(bFunction.apply(e.getKey(),e.getValue())
                                .map(y->new SimpleImmutableEntry<>(null,y))))
                        .map(Polyfill::<Entry<X,Y>>uncheckedCast)
                        .collect(Streams.batches(2))
                var b = ;
            }
        }
         */

        //endregion
        //region cast
        @WrapWith("flatMap")
        public <X, Y> Function<Entry<?, Y>, Stream<Entry<X, Y>>> castA(final @NotNull Class<X> type) {
            return e -> Stream.of(e.getKey()).flatMap(Streams.cast(type)).map(a -> new SimpleImmutableEntry<>(a, e.getValue()));
        }

        @WrapWith("flatMap")
        public <X, Y> Function<Entry<X, ?>, Stream<Entry<X, Y>>> castB(final @NotNull Class<Y> type) {
            return e -> Stream.of(e.getValue()).flatMap(Streams.cast(type)).map(b -> new SimpleImmutableEntry<>(e.getKey(), b));
        }

        @WrapWith("flatMap")
        public <X, Y> Function<Entry<?, ?>, Stream<Entry<X, Y>>> cast(final @NotNull Class<X> aType, final @NotNull Class<Y> bType) {
            return e -> Stream.of(e).flatMap(filter(aType::isInstance, bType::isInstance)).map(map(aType::cast, bType::cast));
        }

        //endregion
        //region forEach
        @WrapWith("forEach")
        public <T> Consumer<Entry<T, T>> forEachMono(final @NotNull Consumer<T> consumer) {
            return forEach(consumer, consumer);
        }

        @WrapWith("forEach")
        public <A, B> Consumer<Entry<A, B>> forEach(final @NotNull Consumer<A> aConsumer, final @NotNull Consumer<B> bConsumer) {
            return forEach((a, b) -> {
                aConsumer.accept(a);
                bConsumer.accept(b);
            });
        }

        @WrapWith("forEach")
        public <A, B> Consumer<Entry<A, B>> forEach(final @NotNull BiConsumer<A, B> consumer) {
            return e -> consumer.accept(e.getKey(), e.getValue());
        }

        //endregion
        //region collector
        @WrapWith("collect")
        public <K, V> Collector<Entry<K, V>, Map<K, V>, Map<K, V>> collector() {
            return collector(HashMap::new);
        }

        @WrapWith("collect")
        public <K, V> Collector<Entry<K, V>, Map<K, V>, Map<K, V>> collector(final Supplier<Map<K, V>> mapSupplier) {
            return Collector.of(mapSupplier, (m, e) -> m.put(e.getKey(), e.getValue()), (a, b) -> {
                a.putAll(b);
                return a;
            }, Collections::unmodifiableMap);
        }
        //endregion

        @Retention(RetentionPolicy.CLASS)
        private @interface WrapWith {
            String value();
        }

        @Value
        public static class Adapter<A, B, X, Y, I, O> {
            public static <A, B, X> Adapter<A, B, X, B, A, X> sideA() {
                return new Adapter<>(Entry::getKey, (e, x) -> x, (e, y) -> e.getValue());
            }

            public static <A, B, Y> Adapter<A, B, A, Y, B, Y> sideB() {
                return new Adapter<>(Entry::getValue, (e, y) -> e.getKey(), (e, y) -> y);
            }

            public static <A, B, X, Y> Adapter<A, B, X, Y, Entry<A, B>, Entry<X, Y>> tunnel() {
                return new Adapter<>(identity(), ($, e) -> e.getKey(), ($, e) -> e.getValue());
            }

            Function<Entry<A, B>, I>      input;
            BiFunction<Entry<A, B>, O, X> outputX;
            BiFunction<Entry<A, B>, O, Y> outputY;

            private Entry<X, Y> merge(Entry<A, B> entry, O output) {
                return new SimpleImmutableEntry<>(outputX.apply(entry, output), outputY.apply(entry, output));
            }
        }
    }
}
