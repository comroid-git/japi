package org.comroid.api;

public interface MutableState {
    /**
     * Checks whether this container object is currently allowed to change its value.
     *
     * @return whether this container object is currently allowed to change its value
     */
    boolean isMutable();

    /**
     * Checks whether this container object is currently prohibited to change its value.
     *
     * @return whether this container object is currently prohibited to change its value.
     */
    default boolean isImmutable() {
        return !isMutable();
    }

    /**
     * Attempts to change the mutability state of this container object.
     * <p>
     * If the return value is {@code true}, the new {@code state} could be applied.
     * Returns {@code false} if otherwise.
     *
     * @param state The new mutability state
     * @return Whether the new mutability state could be applied
     */
    boolean setMutable(boolean state);

    /**
     * Attempts to allow mutation of this container object.
     *
     * @throws UnsupportedOperationException if this object could not be made mutable. May be caused by object already being mutable.
     */
    default void setMutable() throws UnsupportedOperationException {
        if (!setMutable(true))
            throw new UnsupportedOperationException("Could not make " + this + " mutable");
    }

    /**
     * Attempts to disallow mutation of this container object.
     *
     * @throws UnsupportedOperationException if this object could not be made immutable. May be caused by object already being mutable.
     */
    default void setImmutable() {
        if (!setMutable(false))
            throw new UnsupportedOperationException("Could not make " + this + " immutable");
    }
}
