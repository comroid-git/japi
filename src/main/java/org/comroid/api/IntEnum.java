package org.comroid.api;

import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public interface IntEnum extends Named, ValueBox<Integer> {
    @Override
    default @NotNull Integer getValue() {
        if (this instanceof Enum)
            return ((Enum<?>) this).ordinal();
        throw new AbstractMethodError();
    }

    @Override
    default String getName() {
        if (this instanceof Enum)
            return ((Enum<?>) this).name();
        throw new AbstractMethodError();
    }

    @Override
    default ValueType<? extends Integer> getHeldType() {
        return StandardValueType.INTEGER;
    }

    static <T extends java.lang.Enum<? extends T> & IntEnum> Rewrapper<T> valueOf(int value, Class<T> viaEnum) {
        if (!viaEnum.isEnum())
            throw new IllegalArgumentException("Only enums allowed as parameter 'viaEnum'");

        return valueOf(value, viaEnum, Class::getEnumConstants);
    }

    static <T extends IntEnum> Rewrapper<T> valueOf(
            int value,
            Class<T> viaClass,
            Function<Class<T>, T[]> valuesProvider) {
        final T[] constants = valuesProvider.apply(viaClass);

        for (T it : constants)
            if (it.getValue() == value)
                return () -> it;
        return Rewrapper.empty();
    }

    default boolean equals(int value) {
        return getValue() == value;
    }
}
