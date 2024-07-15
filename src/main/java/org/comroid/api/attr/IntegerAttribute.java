package org.comroid.api.attr;

import org.comroid.api.data.seri.type.StandardValueType;
import org.comroid.api.data.seri.type.ValueType;
import org.comroid.api.func.ValueBox;
import org.comroid.api.func.ext.Wrap;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.function.IntSupplier;

/**
 * An attribute interface for objects that contains a plain integer value.
 *
 * @see Named
 */
public interface IntegerAttribute extends Named, ValueBox<@NotNull Integer>, IntSupplier, Index {
    Comparator<IntegerAttribute> COMPARATOR = Comparator.comparingInt(IntegerAttribute::getAsInt);

    /**
     * Returns a {@link Wrap} supplying an instance of the corresponding attribute within the given enum class.
     *
     * @param value   The integer value
     * @param viaEnum The enum the check it's attributes
     * @param <T>     The enum type
     *
     * @return A Rewrapper that supplies the result attribute, or an empty rewrapper.
     */
    static <T extends java.lang.Enum<? extends T> & IntegerAttribute> Wrap<T> valueOf(int value, Class<T> viaEnum) {
        if (!viaEnum.isEnum())
            throw new IllegalArgumentException("Only enums allowed as parameter 'viaEnum'");

        return valueOf(value, viaEnum.getEnumConstants());
    }

    /**
     * Returns a {@link Wrap} supplying an instance of the corresponding attribute within the given enum class.
     *
     * @param value     The integer value
     * @param constants All possible values
     * @param <T>       The enum type
     *
     * @return A Rewrapper that supplies the result attribute, or an empty rewrapper.
     */
    static <T extends IntegerAttribute> Wrap<T> valueOf(int value, final T[] constants) {
        for (T it : constants)
            if (it.getValue() == value)
                return () -> it;
        return Wrap.empty();
    }

    @Override
    default int getAsInt() {
        return getValue();
    }

    /**
     * Default implementation to obtain the {@link StandardValueType#INTEGER default integer value type} for {@link ValueBox}.
     *
     * @return {@link StandardValueType#INTEGER}
     */
    @Override
    default ValueType<? extends Integer> getHeldType() {
        return StandardValueType.INTEGER;
    }

    /**
     * Default implementation. If this is an instance of {@link Enum}, the {@linkplain Enum#ordinal() enum ordinal} is returned.
     * Otherwise, an {@link AbstractMethodError} will be thrown.
     *
     * @return The integer value
     * @throws AbstractMethodError If this is not an {@link Enum} instance
     */
    @Override
    default @NotNull Integer getValue() throws AbstractMethodError {
        if (this instanceof Enum)
            return ((Enum<?>) this).ordinal();
        throw new AbstractMethodError();
    }

    @Override
    default int index() {
        return getValue();
    }

    /**
     * An equals-implementation to accept int values.
     *
     * @param value The value to check
     *
     * @return Whether this objects value and the other value are equal.
     */
    default boolean equals(int value) {
        return getValue() == value;
    }
}
