package org.comroid.api;

public interface EnabledState {
    default boolean isEnabled() {
        return true;
    }
}
