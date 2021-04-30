package org.comroid.api;

/**
 * An attribute interface in order to obtain common Names from an object using {@link #getName()} and {@link #getAlternateName()}.
 * <p>
 * If used within an enum class, the returned Name will correspond to the {@linkplain Enum#name() enum constant's name}, otherwise {@link #toString()}.
 *
 * @see WrappedFormattable as this class extends {@link java.util.Formattable}
 */
public interface Named extends WrappedFormattable {
    /**
     * Default implementation to get the primary name.
     * If this is an instance of {@link Enum}, returns its {@link Enum#name() enum constant name}.
     *
     * @return The primary name.
     */
    @Override
    default String getPrimaryName() {
        if (this instanceof Enum)
            return ((Enum<?>) this).name();
        return getAlternateName();
    }

    /**
     * Default implementation to get the alternate name. Calls {@link #toString()}.
     *
     * @return The alternate name.
     */
    @Override
    default String getAlternateName() {
        return toString();
    }

    /**
     * Returns the primary common name of this object.
     * By default, invokes {@link #getPrimaryName()}.
     *
     * @return the primary common name.
     */
    default String getName() {
        return getPrimaryName();
    }

    /**
     * A base class for a Named object using a final name field.
     */
    class Base implements Named {
        private final String name;

        @Override
        public final String getName() {
            return name;
        }

        protected Base(String name) {
            this.name = name;
        }
    }
}
