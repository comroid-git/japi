package org.comroid.api;

import org.comroid.annotations.Ignore;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.NotNull;

import java.util.function.LongSupplier;

/**
 * An attribute interface for objects that contains a plain long value.
 *
 * @see Named
 */
public interface LongAttribute extends Named, ValueBox<@NotNull Long>, LongSupplier, Index {
    @Override
    default int index() {
        return Math.toIntExact(getValue());
    }

    @Ignore
    @Override
    default long getAsLong() {
        return getValue();
    }

    /**
     * Default implementation. If this is an instance of {@link Enum}, the {@linkplain Enum#ordinal() enum ordinal} is returned.
     * Otherwise, an {@link AbstractMethodError} will be thrown.
     *
     * @return The long value
     * @throws AbstractMethodError If this is not an {@link Enum} instance
     */
    @Override
    default @NotNull Long getValue() throws AbstractMethodError {
        if (this instanceof Enum)
            return (long) ((Enum<?>) this).ordinal();
        throw new AbstractMethodError();
    }

    /**
     * Default implementation to obtain the {@link StandardValueType#LONG default long value type} for {@link ValueBox}.
     *
     * @return {@link StandardValueType#LONG}
     */
    @Override
    default ValueType<? extends Long> getHeldType() {
        return StandardValueType.LONG;
    }

    /**
     * Returns a {@link SupplierX} supplying an instance of the corresponding attribute within the given enum class.
     *
     * @param value   The long value
     * @param viaEnum The enum the check it's attributes
     * @param <T>     The enum type
     * @return A Rewrapper that supplies the result attribute, or an empty rewrapper.
     */
    static <T extends Enum<? extends T> & LongAttribute> SupplierX<T> valueOf(long value, Class<T> viaEnum) {
        if (!viaEnum.isEnum())
            throw new IllegalArgumentException("Only enums allowed as parameter 'viaEnum'");

        return valueOf(value, viaEnum.getEnumConstants());
    }


    /**
     * Returns a {@link SupplierX} supplying an instance of the corresponding attribute within the given enum class.
     *
     * @param value     The long value
     * @param constants All possible values
     * @param <T>       The enum type
     * @return A Rewrapper that supplies the result attribute, or an empty rewrapper.
     */
    static <T extends LongAttribute> SupplierX<T> valueOf(long value, final T[] constants) {
        for (T it : constants)
            if (it.getValue() == value)
                return () -> it;
        return SupplierX.empty();
    }

    /**
     * An equals-implementation to accept long values.
     *
     * @param value The value to check
     * @return Whether this objects value and the other value are equal.
     */
    default boolean equals(long value) {
        return getValue() == value;
    }
}
