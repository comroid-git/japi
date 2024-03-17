package org.comroid.api.func.util;

import lombok.Value;
import lombok.experimental.UtilityClass;
import org.comroid.annotations.Convert;
import org.comroid.annotations.Default;
import org.comroid.annotations.Instance;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.LongAttribute;
import org.comroid.api.attr.Named;
import org.comroid.api.func.ext.SelfDeclared;
import org.comroid.api.java.StackTraceUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@UtilityClass
public final class Bitmask {
    public static final long EMPTY = 0x0;
    private static final Map<Class<?>, AtomicLong> LAST_FLAG = new ConcurrentHashMap<>();

    public static long combine(Bitmask.Attribute<?>... values) {
        long yield = EMPTY;

        for (Bitmask.Attribute<?> value : values)
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

    public static boolean isFlagSet(long mask, Bitmask.Attribute<?> attribute) {
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
        return new Collector<>() {
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
        };
    }
    
    /**
     * Helper interface for enum classes that store an long bitmask.
     * <p>
     * The default generated bitmasks are dependent on the order of constants.
     *
     * @param <S> The implementing Enum type
     * @see #getValue() for further information
     * @see Named Default Enum implementation
     */
    public interface Attribute<S> extends LongAttribute, SelfDeclared<S>, Named {
        /**
         * Computes a default long value for this bitmask, depending on enum order.
         * If implemented by an enum class, this method provides unique default bitmasks for every enum constant.
         *
         * @return The long value of this Bitmask constant.
         * @see LongAttribute#getValue() Returns Enums ordinal value if possible
         */
        @Override
        default @NotNull Long getValue() {
            return 1L << LongAttribute.super.getValue();
        }

        /**
         * Creates a set of all mask attributes from an long value and an enum class.
         *
         * @param mask    The long value to scan
         * @param viaEnum The enum to use all constants from.
         * @param <T>     The enum type.
         * @return A set of all Bitmask attributes set in the long value
         */
        static <T extends java.lang.Enum<? extends T> & Bitmask.Attribute<T>> Set<T> valueOf(long mask, Class<T> viaEnum) {
            if (!viaEnum.isEnum())
                throw new IllegalArgumentException("Only enums allowed as parameter 'viaEnum'");

            return valueOf(mask, viaEnum.getEnumConstants());
        }


        /**
         * Creates a set of all mask attributes from an long value and an enum class.
         *
         * @param mask   The long value to scan
         * @param values All possible mask attributes
         * @param <T>    The enum type.
         * @return A set of all Bitmask attributes set in the long value
         */
        static <T extends Bitmask.Attribute<T>> Set<T> valueOf(long mask, T[] values) {
            HashSet<T> yields = new HashSet<>();

            for (T constant : values) {
                if (constant.isFlagSet(mask))
                    yields.add(constant);
            }

            return Collections.unmodifiableSet(yields);
        }

        /**
         * Creates an long value containing all provided Bitmask attributes.
         *
         * @param values All values to combine
         * @return The result long value
         */
        static long toMask(Bitmask.Attribute<?>[] values) {
            long x = 0;
            for (Bitmask.Attribute<?> each : values)
                x = each.apply(x, true);
            return x;
        }

        /**
         * Checks whether this Bitmask attribute contains another attribute.
         *
         * @param other The other attribute.
         * @return Whether the other attribute is contained in this attribute
         */
        default boolean hasFlag(Bitmask.Attribute<S> other) {
            return Bitmask.isFlagSet(getValue(), other.getValue());
        }

        /**
         * Checks whether this attribute is set within an long mask.
         *
         * @param inMask The mask to check.
         * @return Whether this attribute is contained in the mask
         */
        default boolean isFlagSet(long inMask) {
            return Bitmask.isFlagSet(inMask, getValue());
        }

        /**
         * Applies the {@code newState} of this attribute to the given mask, and returns the result.
         *
         * @param toMask   The mask to apply this attribute to
         * @param newState The desired state of this attribute within the mask
         * @return The new mask
         */
        default long apply(long toMask, boolean newState) {
            return Bitmask.modifyFlag(toMask, getValue(), newState);
        }

        /**
         * {@linkplain Object#equals(Object) Equals-implementation} to accept instances of Bitmask.Attribute
         *
         * @param other The attribute to check against.
         * @return Whether the attribute values are equal
         */
        default boolean equals(Bitmask.Attribute<?> other) {
            return getValue() == (long) other.getValue();
        }
    }

    @Value
    public static class List<S extends Bitmask.Attribute<S>> extends ArrayList<@NotNull Long> implements Bitmask.Attribute<S> {
        private static final List<?> EMPTY = new List<>();

        @Instance @Default
        public static <S extends Attribute<S>> List<S> empty() {
            return Polyfill.uncheckedCast(EMPTY);
        }

        private List() {
            this(new long[0]);
        }

        @Convert
        public List(long... flags) {
            this(LongStream.of(flags).boxed().toList());
        }

        @Convert
        public List(@NotNull Collection<? extends @NotNull Long> c) {
            super(c);
        }

        @Override
        public @NotNull Long getValue() {
            return stream().collect(collector());
        }

        @Override
        public Stream<@NotNull Long> stream() {
            return super.stream();
        }

        public LongStream longStream() {
            return stream().mapToLong(x->x);
        }
    }
}
