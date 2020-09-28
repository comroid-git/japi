package org.comroid.api;

import org.comroid.util.Bitmask;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public interface BitmaskEnum<S extends BitmaskEnum<S>> extends IntEnum, SelfDeclared<S> {
    @Override
    int getValue();

    static <T extends java.lang.Enum<? extends T> & BitmaskEnum<T>> Set<T> valueOf(int mask, Class<T> viaEnum) {
        return valueOf(mask, viaEnum, Class::getEnumConstants);
    }

    static <T extends java.lang.Enum<? extends T> & BitmaskEnum<T>> Set<T> valueOf(
            int mask,
            Class<T> viaEnum,
            Function<Class<T>, T[]> valuesProvider) {
        if (!viaEnum.isEnum())
            throw new IllegalArgumentException("Only enums allowed as parameter 'viaEnum'");

        final T[] constants = valuesProvider.apply(viaEnum);
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
}
