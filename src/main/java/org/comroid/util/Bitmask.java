package org.comroid.util;

import org.comroid.api.BitmaskAttribute;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import java.util.stream.Collector;

public final class Bitmask {
    public static final long EMPTY = 0x0;
    private static final Map<Class<?>, AtomicLong> LAST_FLAG = new ConcurrentHashMap<>();

    public static long combine(BitmaskAttribute<?>... values) {
        long yield = EMPTY;

        for (BitmaskAttribute<?> value : values)
            yield = value.apply(yield, true);

        return yield;
    }

    public static long modifyFlag(long mask, long flag, boolean newState) {
        final boolean isSet = isFlagSet(mask, flag);
        if (!isSet && newState) {
            // add flag
            return mask | flag;
        } else if (isSet && !newState) {
            // remove flag
            return mask & ~flag;
        } else return mask; // do nothing
    }

    public static boolean isFlagSet(long mask, BitmaskAttribute<?> attribute) {
        return isFlagSet(mask, attribute.getAsLong());
    }

    public static boolean isFlagSet(long mask, long flag) {
        return (mask & flag) != 0;
    }

    //@CallerSensitive
    public static long nextFlag() {
        return nextFlag(1);
    }

    //@CallerSensitive
    public static long nextFlag(int traceDelta) {
        return nextFlag(StackTraceUtils.callerClass(traceDelta));
    }

    public static long nextFlag(Class<?> type) {
        final AtomicLong atom = LAST_FLAG.computeIfAbsent(type, key -> new AtomicLong(-1));
        atom.accumulateAndGet(1, Long::sum);
        return 1 << atom.get();
    }

    public static long combine(long... masks) {
        long yield = EMPTY;

        for (long mask : masks) {
            yield = yield | mask;
        }

        return yield;
    }

    @SafeVarargs
    public static <T> long combine(ToLongFunction<T> mapper, T... items) {
        long yield = EMPTY;

        for (T item : items) {
            yield = yield | mapper.applyAsLong(item);
        }

        return yield;
    }

    public static long arrange(boolean... bits) {
        var x = 0;
        for (int i = 0; i < bits.length; i++)
            x |= (bits[i]?1:0) << i;
        return x;
    }

    public static Collector<Long, AtomicLong, Long> collector() {
        return new BitmaskCollector();
    }

    private static final class BitmaskCollector implements Collector<Long, AtomicLong, Long> {
        private static final Set<Characteristics> characteristics
                = Collections.singleton(Characteristics.IDENTITY_FINISH);
        private final Supplier<AtomicLong> supplier
                = () -> new AtomicLong(0);
        private final BiConsumer<AtomicLong, Long> accumulator
                = (atom, x) -> atom.accumulateAndGet(x, Bitmask::combine);
        private final BinaryOperator<AtomicLong> combiner = (x, y) -> {
            x.accumulateAndGet(y.get(), Bitmask::combine);
            return x;
        };
        private final Function<AtomicLong, Long> finisher = AtomicLong::get;

        @Override
        public Supplier<AtomicLong> supplier() {
            return supplier;
        }

        @Override
        public BiConsumer<AtomicLong, Long> accumulator() {
            return accumulator;
        }

        @Override
        public BinaryOperator<AtomicLong> combiner() {
            return combiner;
        }

        @Override
        public Function<AtomicLong, Long> finisher() {
            return finisher;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }
    }
}
