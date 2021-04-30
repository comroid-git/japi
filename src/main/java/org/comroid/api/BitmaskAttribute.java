package org.comroid.api;

import org.comroid.util.Bitmask;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper interface for enum classes that store an integer bitmask.
 * <p>
 * The default generated bitmasks are dependent on the order of constants.
 *
 * @param <S> The implementing Enum type
 * @see #getValue() for further information
 * @see Named Default Enum implementation
 */
public interface BitmaskAttribute<S extends BitmaskAttribute<S>> extends IntegerAttribute, SelfDeclared<S>, Named {
    /**
     * Computes a default integer value for this bitmask, depending on enum order.
     * If implemented by an enum class, this method provides unique default bitmasks for every enum constant.
     *
     * @return The integer value of this Bitmask constant.
     * @see IntegerAttribute#getValue() Returns Enums ordinal value if possible
     */
    @Override
    @NotNull
    default Integer getValue() {
        return 1 << IntegerAttribute.super.getValue();
    }

    /**
     * Creates a set of all mask attributes from an integer value and an enum class.
     *
     * @param mask    The integer value to scan
     * @param viaEnum The enum to use all constants from.
     * @param <T>     The enum type.
     * @return A set of all Bitmask attributes set in the integer value
     */
    static <T extends java.lang.Enum<? extends T> & BitmaskAttribute<T>> Set<T> valueOf(int mask, Class<T> viaEnum) {
        if (!viaEnum.isEnum())
            throw new IllegalArgumentException("Only enums allowed as parameter 'viaEnum'");

        return valueOf(mask, viaEnum.getEnumConstants());
    }


    /**
     * Creates a set of all mask attributes from an integer value and an enum class.
     *
     * @param mask   The integer value to scan
     * @param values All possible mask attributes
     * @param <T>    The enum type.
     * @return A set of all Bitmask attributes set in the integer value
     */
    static <T extends BitmaskAttribute<T>> Set<T> valueOf(int mask, T[] values) {
        HashSet<T> yields = new HashSet<>();

        for (T constant : values) {
            if (constant.isFlagSet(mask))
                yields.add(constant);
        }

        return Collections.unmodifiableSet(yields);
    }

    /**
     * Creates an integer value containing all provided Bitmask attributes.
     *
     * @param values All values to combine
     * @return The result integer value
     */
    static int toMask(BitmaskAttribute<?>[] values) {
        int x = 0;
        for (BitmaskAttribute<?> each : values)
            x = each.apply(x, true);
        return x;
    }

    /**
     * Checks whether this Bitmask attribute contains another attribute.
     *
     * @param other The other attribute.
     * @return Whether the other attribute is contained in this attribute
     */
    default boolean hasFlag(BitmaskAttribute<S> other) {
        return Bitmask.isFlagSet(getValue(), other.getValue());
    }

    /**
     * Checks whether this attribute is set within an integer mask.
     *
     * @param inMask The mask to check.
     * @return Whether this attribute is contained in the mask
     */
    default boolean isFlagSet(int inMask) {
        return Bitmask.isFlagSet(inMask, getValue());
    }

    /**
     * Applies the {@code newState} of this attribute to the given mask, and returns the result.
     *
     * @param toMask   The mask to apply this attribute to
     * @param newState The desired state of this attribute within the mask
     * @return The new mask
     */
    default int apply(int toMask, boolean newState) {
        return Bitmask.modifyFlag(toMask, getValue(), newState);
    }

    /**
     * {@linkplain Object#equals(Object) Equals-implementation} to accept instances of BitmaskAttribute
     *
     * @param other The attribute to check against.
     * @return Whether the attribute values are equal
     */
    default boolean equals(BitmaskAttribute<?> other) {
        return getValue() == (int) other.getValue();
    }
}
