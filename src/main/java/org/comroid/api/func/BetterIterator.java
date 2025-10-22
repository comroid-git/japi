package org.comroid.api.func;

import lombok.Value;
import lombok.experimental.Delegate;
import lombok.experimental.NonFinal;

import java.util.ListIterator;

@Value
public class BetterIterator<T> implements ListIterator<T> {
    @Delegate ListIterator<T> iterator;
    @NonFinal T               current;

    public BetterIterator(ListIterator<T> iterator) {
        this.iterator = iterator;
    }

    @Override
    public T next() {
        return current = iterator.next();
    }

    @Override
    public T previous() {
        return current = iterator.previous();
    }
}
