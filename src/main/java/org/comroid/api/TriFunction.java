package org.comroid.api;

public interface TriFunction<P, T, X, R> {
    R apply(P p1, T p2, X p3);
}
