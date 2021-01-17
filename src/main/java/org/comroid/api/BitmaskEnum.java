package org.comroid.api;

import org.comroid.util.Bitmask;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public interface BitmaskEnum<S extends BitmaskEnum<S>> extends IntEnum, SelfDeclared<S>, Named {
    @Override
    int getValue();

    static <T extends java.lang.Enum<? extends T> & BitmaskEnum<T>> Set<T> valueOf(int mask, Class<T> viaEnum) {
        if (!viaEnum.isEnum())
            throw new IllegalArgumentException("Only enums allowed as parameter 'viaEnum'");

        return valueOf(mask, viaEnum, Class::getEnumConstants);
    }

    static <T extends BitmaskEnum<T>> Set<T> valueOf(
            int mask,
            Class<? super T> viaEnum,
            Function<Class<? extends T>, T[]> valuesProvider) {
        final T[] constants = valuesProvider.apply(Polyfill.uncheckedCast(viaEnum));
        HashSet<T> yields = new HashSet<>();

        for (T constant : constants) {
            if (constant.isFlagSet(mask))
                yields.add(constant);
        }

        return Collections.unmodifiableSet(yields);
    }

    default boolean hasFlag(BitmaskEnum<S> other) {
        return Bitmask.isFlagSet(getValue(), other.getValue());
    }

    default boolean isFlagSet(int inMask) {
        return Bitmask.isFlagSet(inMask, getValue());
    }

    default int apply(int toMask, boolean newState) {
        return Bitmask.modifyFlag(toMask, getValue(), newState);
    }

    @Override
    default boolean equals(int value) {
        return getValue() == value;
    }

    default boolean equals(BitmaskEnum other) {
        return getValue() == other.getValue();
    }
}
