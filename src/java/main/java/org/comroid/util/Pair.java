package org.comroid.util;

import java.util.concurrent.atomic.AtomicReference;

public class Pair<A, B> {
    protected final AtomicReference<A> first;
    protected final AtomicReference<B> second;

    public A getFirst() {
        return first.get();
    }

    public B getSecond() {
        return second.get();
    }

    public Pair(A first, B second) {
        this.first = new AtomicReference<>(first);
        this.second = new AtomicReference<>(second);
    }

    @Override
    public String toString() {
        return String.format("Pair{first=%s, second=%s}", getFirst(), getSecond());
    }
}
