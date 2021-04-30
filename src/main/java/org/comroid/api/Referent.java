package org.comroid.api;

public interface Referent<T> {
    /**
     * Checks whether the current value is {@code null}.
     *
     * @return Whether the currently held value is equal to {@code null}
     */
    boolean isNull();

    /**
     * Checks whether the current value is not {@code null}.
     *
     * @return Whether the currently held value is not equal to {@code null}
     */
    boolean isNonNull();
}
