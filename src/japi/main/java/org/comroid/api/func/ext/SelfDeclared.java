package org.comroid.api.func.ext;

import org.comroid.api.func.ext.Wrap;

/**
 * An interface that declares itself as a type parameter
 *
 * @param <S> The implementing type
 */
public interface SelfDeclared<S> {
    default Wrap<? extends S> self() {
        //noinspection unchecked
        return () -> (S) this;
    }
}
