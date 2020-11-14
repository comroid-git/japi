package org.comroid.api;

/**
 * fuck naming
 */
public interface SelfDeclared<S extends SelfDeclared<? super S>> {
    default Rewrapper<S> self() {
        //noinspection unchecked
        return () -> (S) this;
    }
}
