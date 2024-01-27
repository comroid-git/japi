package org.comroid.api.attr;

public interface EnabledState {
    default boolean isEnabled() {
        return true;
    }
}
