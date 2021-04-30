package org.comroid.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.comroid.api.BitmaskAttribute;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collector;

public final class Bitmask {
    public static final int EMPTY = 0x0;
    private static final Map<Class<?>, AtomicInteger> LAST_FLAG = new ConcurrentHashMap<>();

    public static int combine(BitmaskAttribute<?>... values) {
        int yield = EMPTY;

        for (BitmaskAttribute<?> value : values)
            yield = value.apply(yield, true);

        return yield;
    }

    public static int modifyFlag(int mask, int flag, boolean newState) {
        final boolean isSet = isFlagSet(mask, flag);
        if (!isSet && newState) {
            // add flag
            return mask | flag;
        } else if (isSet && !newState) {
            // remove flag
            return mask & ~flag;
        } else return mask; // do nothing
    }

    public static boolean isFlagSet(int mask, int flag) {
        return (mask & flag) != 0;
    }
    //@CallerSensitive

    public static int nextFlag() {
        return nextFlag(0);
    }
    //@CallerSensitive

    public static int nextFlag(int traceDelta) {
        final AtomicInteger atom = LAST_FLAG.computeIfAbsent(StackTraceUtils
                .callerClass(1 + traceDelta), key -> new AtomicInteger(-1));

        atom.accumulateAndGet(1, Integer::sum);
        return 1 << atom.get();
    }

    public static int combine(int... masks) {
        int yield = EMPTY;

        for (int mask : masks) {
            yield = yield | mask;
        }

        return yield;
    }

    @SafeVarargs
    public static <T> int combine(ToIntFunction<T> mapper, T... items) {
        int yield = EMPTY;

        for (T item : items) {
            yield = yield | mapper.applyAsInt(item);
        }

        return yield;
    }

    public static Collector<Integer, AtomicInteger, Integer> collector() {
        return new BitmaskCollector();
    }

    public static void printIntegerBytes(Logger logger, @Nullable String title, int value) {
        byte[] bytes = ByteBuffer.allocate(4)
                .putInt(value)
                .array();
        printByteArrayDump(logger, String.format("Integer Dump of %s [%d]", title, value), bytes);
    }

    public static void printByteArrayDump(Logger logger, @Nullable String title, byte[] bytes) {
        StringBuilder sb = new StringBuilder("\n");
        for (int i = 0; i < bytes.length; i++) {
            byte each = bytes[i];
            sb.append(createByteDump(null, each));
            if (i % 2 == 1)
                sb.append('\n');
        }
        logger.log(Level.ALL, (title == null ? "" : "Printing byte array dump of " + title) + sb.substring(0, sb.length() - 1));
    }

    public static String createByteDump(@Nullable String title, byte each) {
        StringBuilder binaryString = new StringBuilder(Integer.toUnsignedString(each, 2));
        while (binaryString.length() < 8)
            binaryString.insert(0, '0');
        return String.format("%s0x%2x [0b%s]\t", (title == null ? "" : "Creating byte dump of " + title + '\n'), each, binaryString.toString());
    }

    private static final class BitmaskCollector implements Collector<Integer, AtomicInteger, Integer> {
        private static final Set<Characteristics> characteristics
                = Collections.singleton(Characteristics.IDENTITY_FINISH);
        private final Supplier<AtomicInteger> supplier
                = () -> new AtomicInteger(0);
        private final BiConsumer<AtomicInteger, Integer> accumulator
                = (atom, x) -> atom.accumulateAndGet(x, Bitmask::combine);
        private final BinaryOperator<AtomicInteger> combiner = (x, y) -> {
            x.accumulateAndGet(y.get(), Bitmask::combine);
            return x;
        };
        private final Function<AtomicInteger, Integer> finisher = AtomicInteger::get;

        @Override
        public Supplier<AtomicInteger> supplier() {
            return supplier;
        }

        @Override
        public BiConsumer<AtomicInteger, Integer> accumulator() {
            return accumulator;
        }

        @Override
        public BinaryOperator<AtomicInteger> combiner() {
            return combiner;
        }

        @Override
        public Function<AtomicInteger, Integer> finisher() {
            return finisher;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }
    }
}
