package org.comroid.api.attr;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.comroid.annotations.Ignore;
import org.comroid.api.func.WrappedFormattable;
import org.comroid.api.text.Capitalization;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.Nullable;

/**
 * An attribute interface in order to obtain common Names from an object using {@link #getName()} and {@link #getAlternateName()}.
 * <p>
 * If used within an enum class, the returned Name will correspond to the {@linkplain Enum#name() enum constant's name}, otherwise {@link Object#toString()}.
 *
 * @see WrappedFormattable as this class extends {@link java.util.Formattable}
 */
public interface Named extends WrappedFormattable {
    /**
     * Default implementation to get the primary name.
     * By default, invokes {@link #getName()}.
     *
     * @return The primary name.
     */
    @Ignore
    @Override
    @JsonIgnore
    default String getPrimaryName() {
        return getName();
    }
    /**
     * Default implementation to get the primary name.
     * By default, invokes {@link #getName()}.
     *
     * @return The primary name.
     */
    @Ignore
    @Override
    default String getAlternateName() {
        final var name = getName();
        return Capitalization.of(name).ifPresentMapOrElseGet(
                cap -> cap.convert(Capitalization.Title_Case, name),
                this::toString);
    }

    default String getBestName() {
        var alt = getAlternateName();
        if (alt != null && !alt.isBlank())
            return alt;
        return getName();
    }

    /**
     * Returns the primary common name of this object.
     * If this is an instance of {@link Enum}, returns its {@link Enum#name() enum constant name}.
     *
     * @return the primary common name.
     */
    default String getName() {
        if (this instanceof Enum)
            return ((Enum<?>) this).name();
        return toString();
    }

    @Experimental
    default Object setName(@Nullable String name){return this;}

    static String $(Object any) {
        return any instanceof Named named ? named.getName() : String.valueOf(any);
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
