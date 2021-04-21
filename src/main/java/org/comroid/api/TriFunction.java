package org.comroid.api;

@Deprecated
public interface TriFunction<P, T, X, R> extends NFunction.In3<P, T, X, R> {
    R apply(P p1, T p2, X p3);
}
