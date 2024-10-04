package org.comroid.api.func.util;

import java.util.concurrent.atomic.AtomicReference;

public class Pair<A, B> {
    protected final AtomicReference<A> first;
    protected final AtomicReference<B> second;

    public Pair(A first, B second) {
        this.first = new AtomicReference<>(first);
        this.second = new AtomicReference<>(second);
    }

    public A getFirst() {
        return first.get();
    }

    public B getSecond() {
        return second.get();
    }

    @Override
    public String toString() {
        return String.format("Pair{first=%s, second=%s}", getFirst(), getSecond());
    }
}
