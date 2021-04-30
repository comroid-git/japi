package org.comroid.api;

/**
 * An interface that declares itself as a type parameter
 *
 * @param <S> The implementing type
 */
public interface SelfDeclared<S> {
    default Rewrapper<? extends S> self() {
        //noinspection unchecked
        return () -> (S) this;
    }
}
