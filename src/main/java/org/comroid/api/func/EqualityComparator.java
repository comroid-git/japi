package org.comroid.api.func;

import java.util.Comparator;

@Deprecated
public interface EqualityComparator<T> extends Comparator<T> {
    static <T> EqualityComparator<T> ofComparator(Comparator<T> underlying) {
        if (underlying instanceof EqualityComparator) {
            return (EqualityComparator<T>) underlying;
        }

        return new Support.OfComparator<>(underlying);
    }

    default boolean areEqual(T it, T other) {
        return compare(it, other) == 0;
    }

    @Override
    int compare(T o1, T o2);

    final class Support {
        private static final class OfComparator<T> implements EqualityComparator<T> {
            private final Comparator<T> underlying;

            private OfComparator(Comparator<T> underlying) {
                this.underlying = underlying;
            }

            @Override
            public int compare(T o1, T o2) {
                return underlying.compare(o1, o2);
            }
        }
    }
}
