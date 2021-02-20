package org.comroid.api;

public interface MutableState {
    boolean isMutable();

    default boolean isImmutable() {
        return !isMutable();
    }

    boolean setMutable(boolean state);

    default boolean setMutable() {
        return setMutable(true);
    }

    default boolean setImmutable() {
        return setMutable(false);
    }
}
