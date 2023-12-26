package org.comroid.api;

/**
 * An interface that declares itself as a type parameter
 *
 * @param <S> The implementing type
 */
public interface SelfRef<S> extends Specifiable<S>, Wrap<S> {
}
