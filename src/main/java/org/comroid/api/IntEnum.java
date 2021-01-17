package org.comroid.api;

import java.util.function.Function;

public interface IntEnum extends Named {
    int getValue();

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

    @Override
    default String getName() {
        if (this instanceof Enum)
            return ((Enum<?>) this).name();
        throw new AbstractMethodError();
    }
}
