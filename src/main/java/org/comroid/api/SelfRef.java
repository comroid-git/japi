package org.comroid.api;

import org.jetbrains.annotations.Nullable;

/**
 * An interface that declares itself as a type parameter
 *
 * @param <S> The implementing type
 */
public interface SelfRef<S> extends Specifiable<S>, Rewrapper<S> {
}
