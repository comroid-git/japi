package org.comroid.api.func.util;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.comroid.api.attr.Named;
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

import static java.util.function.Function.identity;
import static java.util.stream.Stream.empty;
import static org.comroid.api.func.N.Consumer.nop;

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

    @Deprecated
    @UtilityClass
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
            return flatMap(Duplex.sideA(), (e, a) -> function.apply(Stream.of(e)));
        }

        @WrapWith("flatMap")
        public <A, B, Y> Function<Entry<A, B>, Stream<Entry<A, Y>>> routeB(final @NotNull Function<Stream<Entry<A, B>>, Stream<Y>> function) {
            return flatMap(Duplex.sideB(), (e, b) -> function.apply(Stream.of(e)));
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
        public <A, B, X> Function<Entry<A, B>, Entry<X, B>> crossB2A(final @NotNull Function<B, X> function) {
            return crossB2A(($, b) -> function.apply(b));
        }

        @WrapWith("map")
        public <A, B, X> Function<Entry<A, B>, Entry<X, B>> crossB2A(final @NotNull BiFunction<A, B, X> function) {
            return cross(function, (a, b) -> b);
        }

        @WrapWith("map")
        public <A, B, X, Y> Function<Entry<A, B>, Entry<X, Y>> cross(final @NotNull BiFunction<A, B, X> xFunction, final @NotNull BiFunction<A, B, Y> yFunction) {
            return e -> new SimpleImmutableEntry<>(xFunction.apply(e.getKey(), e.getValue()), yFunction.apply(e.getKey(), e.getValue()));
        }

        @WrapWith("flatMap")
        public <A, B, R> Function<Entry<A, B>, Stream<R>> merge(final @NotNull BiFunction<A, B, Stream<R>> function) {
            return e -> function.apply(e.getKey(), e.getValue());
        }

        @WrapWith("map")
        public <A, B, R> Function<Entry<A, B>, R> combine(final @NotNull BiFunction<A, B, R> function) {
            return e -> function.apply(e.getKey(), e.getValue());
        }
        //endregion

        //region peek
        @WrapWith("peek")
        public <T> Consumer<Entry<? extends T, ? extends T>> peekMono(final @NotNull Consumer<T> consumer) {
            return peek(consumer, consumer);
        }

        @WrapWith("peek")
        public <A, B> Consumer<Entry<? extends A, ? extends B>> peekA(final @NotNull Consumer<A> consumer) {
            return peek(consumer, nop());
        }

        @WrapWith("peek")
        public <A, B> Consumer<Entry<? extends A, ? extends B>> peekB(final @NotNull Consumer<B> consumer) {
            return peek(nop(), consumer);
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

        //endregion
        //region filter
        @WrapWith("flatMap")
        public <T> Function<Entry<T, T>, Stream<Entry<T, T>>> filterMono(@NotNull Predicate<T> predicate) {
            return filter(OP.LogicalAnd, predicate, predicate);
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
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filter(@NotNull Predicate<A> aPredicate, @NotNull Predicate<B> bPredicate) {
            return filter(OP.LogicalAnd, aPredicate, bPredicate);
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filter(final @NotNull OP op, final @NotNull Predicate<A> aPredicate, final @NotNull Predicate<B> bPredicate) {
            return filter((a, b) -> op.test(() -> aPredicate.test(a), () -> bPredicate.test(b)));
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filter(final @NotNull BiPredicate<A, B> predicate) {
            return filter(predicate, nop());
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filterA(final @NotNull Predicate<A> predicate, final @NotNull Consumer<A> disposal) {
            return filter(predicate, disposal, $ -> true, nop());
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filterB(final @NotNull Predicate<B> predicate, final @NotNull Consumer<B> disposal) {
            return filter($ -> true, nop(), predicate, disposal);
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filter(final @NotNull Predicate<A> aPredicate, final @NotNull Consumer<A> aDisposal, final @NotNull Predicate<B> bPredicate, final @NotNull Consumer<B> bDisposal) {
            return filter(OP.LogicalAnd, aPredicate, aDisposal, bPredicate, bDisposal);
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filter(final @NotNull OP op, final @NotNull Predicate<A> aPredicate, final @NotNull Consumer<A> aDisposal, final @NotNull Predicate<B> bPredicate, final @NotNull Consumer<B> bDisposal) {
            return filter((a, b) -> op.test(() -> aPredicate.test(a), () -> bPredicate.test(b)), (a, b) -> {
                aDisposal.accept(a);
                bDisposal.accept(b);
            });
        }

        @WrapWith("flatMap")
        public <A, B> Function<Entry<A, B>, Stream<Entry<A, B>>> filter(final @NotNull BiPredicate<A, B> predicate, final @NotNull BiConsumer<A, B> disposal) {
            return e -> {
                if (predicate.test(e.getKey(), e.getValue())) return Stream.of(e);
                disposal.accept(e.getKey(), e.getValue());
                return empty();
            };
        }

        //endregion
        //region map
        @WrapWith("map")
        public <I, O> Function<Entry<I, I>, Entry<O, O>> mapMono(final @NotNull Function<I, O> function) {
            return map(function, function);
        }

        @WrapWith("map")
        public <A, B, X> Function<Entry<A, B>, Entry<X, B>> mapA(final @NotNull Function<A, X> function) {
            return map(function, identity());
        }

        @WrapWith("map")
        public <A, B, Y> Function<Entry<A, B>, Entry<A, Y>> mapB(final @NotNull Function<B, Y> function) {
            return map(identity(), function);
        }

        @WrapWith("map")
        public <A, B, X, Y> Function<Entry<A, B>, Entry<X, Y>> map(final @NotNull Function<A, X> axFunction, final @NotNull Function<B, Y> byFunction) {
            return e -> new SimpleImmutableEntry<>(axFunction.apply(e.getKey()), byFunction.apply(e.getValue()));
        }

        //endregion
        //region flatMap
        @WrapWith("flatMap")
        public <A, B, X> Function<Entry<A, B>, Stream<Entry<X, B>>> flatMapA(final @NotNull Function<A, Stream<X>> function) {
            return flatMapA((e, a) -> function.apply(a));
        }

        @WrapWith("flatMap")
        public <A, B, X> Function<Entry<A, B>, Stream<Entry<X, B>>> flatMapA(final @NotNull BiFunction<Entry<A, B>, A, Stream<X>> function) {
            return flatMap(Duplex.sideA(), function);
        }

        @WrapWith("flatMap")
        public <A, B, Y> Function<Entry<A, B>, Stream<Entry<A, Y>>> flatMapB(final @NotNull Function<B, Stream<Y>> function) {
            return flatMapB((e, b) -> function.apply(b));
        }

        @WrapWith("flatMap")
        public <A, B, Y> Function<Entry<A, B>, Stream<Entry<A, Y>>> flatMapB(final @NotNull BiFunction<Entry<A, B>, B, Stream<Y>> function) {
            return flatMap(Duplex.sideB(), function);
        }

        @WrapWith("flatMap")
        public <A, B, X, Y> Function<Entry<A, B>, Stream<Entry<X, Y>>> flatMap(final @NotNull Function<Stream<Entry<A, B>>, Stream<Entry<X, Y>>> function) {
            return flatMap(Duplex.tunnel(), ($, e) -> function.apply(Stream.of(e)));
        }

        @WrapWith("flatMap")
        public <A, B, X, Y, I, O> Function<Entry<A, B>, Stream<Entry<X, Y>>> flatMap(final @NotNull Streams.Duplex<A, B, I, X, O, X, Y> duplex, final @NotNull BiFunction<Entry<A, B>, I, Stream<O>> function) {
            return e -> function.apply(e, duplex.input.apply(e)).map(o -> duplex.merge(e, o));
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
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class Simplex<In, Acc, Out> implements Function<In, Stream<Out>> {
        public static <T> Simplex<T, T, T> peek(final @NotNull Consumer<? super T> action) {
        }

        public static <T> Simplex<T, T, T> filter(final @NotNull Predicate<? super T> filter) {
        }

        public static <I, O> Simplex<I, I, O> map(final @NotNull Function<? super I, ? extends O> function) {
        }

        public static <I, O> Simplex<I, I, O> flatMap(final @NotNull Function<? super I, ? extends Stream<? extends O>> function) {
        }

        public static <R> Simplex<?, ?, R> cast(final @NotNull Class<R> type) {
        }

        public static <T> Simplex<T, ?, ?> forEach(final @NotNull Consumer<? super T> action) {
        }

        public static <InA, InB, R> Simplex<Entry<InA, InB>,?,R> adapter(final @NotNull BiFunction<InA, InB,R> function) {
            return adapter(function, Stream::ofNullable);
        }

        public static <InA, InB, A, R> Simplex<Entry<InA, InB>,A,R> adapter(final @NotNull BiFunction<InA, InB,A> function, final @NotNull Function<@Nullable A,Stream<R>> finalize) {
            return new Simplex<>(e -> function.apply(e.getKey(), e.getValue()), finalize);
        }

        @With
        @NotNull Function<In, Acc> prepare;
        @With
        @NotNull Function<Acc, Stream<Out>> process;

        @Override
        public final Stream<Out> apply(In in) {
            return prepare.andThen(process).apply(in);
        }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class Duplex<In, AccA, AccB, AccS, OutA, OutB, Fin>
            //extends Simplex<In, Entry<AccA,AccB>, Entry<ResA,ResB>, Out>
            implements Function<In, Stream<Fin>> {
        public static <A, B> Duplex<Entry<A, B>, A, B, Entry<A, B>, A, B, Entry<A, B>> peek(BiConsumer<? super A, ? super B> action) {
            return new Duplex<>(
                    Entry::getKey, Entry::getValue,
                    new Adapter<A,B,Entry<A,B>,A,B>(consumer(action),e->Stream.of(e.getKey()),e->Stream.of(e.getValue())),
                    SimpleImmutableEntry::new);
        }

        public static <A, B> Duplex<Entry<A, B>, A, B, Entry<A, B>, A, B, Entry<A, B>> filter(BiPredicate<? super A, ? super B> filter) {
            return new Duplex<>(Entry::getKey, Entry::getValue,
                    Simplex.adapter((a, b) -> filter.test(a,b)?a:null),
                    Simplex.adapter((a, b) -> filter.test(a,b)?b:null),
                    SimpleImmutableEntry::new);
        }

        public static <A, B, X, Y> Duplex<Entry<A, B>, A, B, Entry<A, B>, X, Y, Entry<A, B>> map(BiFunction<? super A, ? super B, Entry<? extends X, ? extends Y>> action) {
            return new Duplex<Entry<A, B>, A, B, Entry<A, B>, X, Y, Entry<A, B>>(Entry::getKey, Entry::getValue,
                    Simplex.<A,B,A>adapter((a,b)->action.apply(a,b)),
                    Simplex.<A,B,B>adapter((a,b)->action.apply(a,b)),
                    SimpleImmutableEntry::new)
        }

        public static <A, B, X, Y> Duplex<Entry<A, B>, A, B, Entry<A, B>, X, Y, Entry<A, B>> flatMap(BiFunction<? super A, ? super B, Stream<Entry<? extends X, ? extends Y>>> action) {
        }

        public static <A, B> Duplex<Entry<A, B>, A, B, Entry<A, B>, A, B, Entry<A, B>> cast(Class<A> typeA, Class<B> typeB) {
        }

        public static <A, B> Duplex<Entry<A, B>, ?, ?, Entry<?, ?>, ?, ?, Entry<A, B>> forEach(BiConsumer<? super A, ? super B> action) {
        }

        public static <T, A, B> Duplex<T, T, ?, Entry<T, ?>, A, B, Entry<A, B>> expand(Function<? super T, ? extends A> extractA, Function<? super T, ? extends A> extractB) {
        }

        public static <A, B, R> Duplex<Entry<A, B>, A, B, Entry<A, B>, R, ?, R> merge(BiFunction<? super A, ? super B, ? super R> merge) {
        }

        public static <In, AccA, AccB, ProcA, ProcB, YieldA, YieldB, ResA, ResB, Out> Duplex<In, AccA, AccB, Entry<AccA, AccB>, ResA, ResB, Out> split(Simplex<AccA, ProcA, ResA> a, Simplex<AccB, ProcB, ResB> b) {
        }

        @With
        @NotNull Function<In, AccA> extractA;
        @With
        @NotNull Function<In, AccB> extractB;
        @With
        @NotNull BiFunction<AccA,AccB, Stream<OutA>> sideA;
        @With
        @NotNull BiFunction<AccA,AccB, Stream<OutB>> sideB;
        @With
        @NotNull BiFunction<OutA,OutB, Fin> finalize;
        @With@NotNull AtomicReference<AccS> cache = new AtomicReference<>();
        @With@NotNull BiFunction<AccA, AccB, AccS> combine;
        @With@NotNull Function<AccS, Stream<OutA>> extractA;
        @With@NotNull Function<AccS, Stream<OutB>> extractB;
        @With
        @NotNull OP linkage;

        public <Convert> Duplex(@NotNull Function<In, AccA> extractA,
                      @NotNull Function<In, AccB> extractB,
                      @NotNull Adapter<AccA,AccB,Convert,OutA,OutB> adapter,
                      @NotNull BiFunction<OutA, OutB, Fin> finalize) {
            this(extractA, extractB, adapter, finalize, OP.BitwiseOr);
        }

        public <Convert> Duplex(@NotNull Function<In, AccA> extractA,
                      @NotNull Function<In, AccB> extractB,
                      @NotNull Adapter<AccA,AccB,Convert,OutA,OutB> adapter,
                      @NotNull BiFunction<OutA, OutB, Fin> finalize,
                      @NotNull OP linkage) {
            this.extractA = extractA;
            this.extractB = extractB;
            this.sideA = adapter::sideA;
            this.sideB = adapter::sideB;
            this.finalize = finalize;
            this.linkage = linkage;
        }

        @Override
        public final Stream<Fin> apply(In in) {
            class BiSpliterator implements Spliterator<Fin> {
                final Spliterator<OutA> outA;
                final Spliterator<OutB> outB;
                final Queue<OutA> queueA = new LinkedList<>();
                final Queue<OutB> queueB = new LinkedList<>();
                @SuppressWarnings("unused")
                final Object lock = new Object();

                public BiSpliterator(Spliterator<OutA> outA, Spliterator<OutB> outB) {
                    this.outA = outA;
                    this.outB = outB;
                }

                @Synchronized("lock")
                private void advanceA(OutA a, Consumer<? super Fin> handler) {
                    if (queueB.isEmpty()) queueA.add(a);
                    else handler.accept(finalize.apply(a, queueB.poll()));
                }

                @Synchronized("lock")
                private void advanceB(OutB b, Consumer<? super Fin> handler) {
                    if (queueA.isEmpty()) queueB.add(b);
                    else handler.accept(finalize.apply(queueA.poll(), b));
                }

                @Override
                public boolean tryAdvance(final Consumer<? super Fin> action) {
                    return linkage.test(
                            () -> outA.tryAdvance(a -> advanceA(a, action)),
                            () -> outB.tryAdvance(b -> advanceB(b, action)));
                }

                @Override
                public Spliterator<Fin> trySplit() {
                    return null;
                }

                @Override
                public long estimateSize() {
                    return Math.max(outA.estimateSize(), outB.estimateSize());
                }

                @Override
                public int characteristics() {
                    return outA.characteristics() | outB.characteristics();
                }
            }
            var a = extractA.apply(in);
            var b = extractB.apply(in);
            return of(new BiSpliterator(
                    sideA.apply(a,b).spliterator(),
                    sideB.apply(a,b).spliterator()));
        }

        private static <A,B,R> BiFunction<A,B,R> consumer(final @NotNull BiConsumer<?super A,?super B> action) {
            return (a,b)-> {
                action.accept(a, b);
                return null;
            };
        }

        @Value
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Adapter<AccA,AccB,AccS,OutA,OutB> {

            public Stream<OutA> sideA(AccA a, AccB b) {
                return extract(extractA, a, b);
            }

            public Stream<OutB> sideB(AccA a, AccB b) {
                return extract(extractB, a, b);
            }

            @Synchronized("cache")
            private <R> Stream<R> extract(final @NotNull Function<AccS, Stream<R>> extract, AccA a, AccB b) {
                var x = cache.get();
                if (x == null)
                    cache.set(x = combine.apply(a, b));
                return extract.apply(x);
            }
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
        public final java.util.stream.Collector<@NotNull BooleanSupplier, @NotNull Boolean, @NotNull Boolean> Collector
                = java.util.stream.Collector.of(() -> false, (l, r) -> test(() -> l, r), (l, r) -> test(() -> l, () -> r));

        @Override
        public abstract boolean test(BooleanSupplier l, BooleanSupplier r);
    }
}
