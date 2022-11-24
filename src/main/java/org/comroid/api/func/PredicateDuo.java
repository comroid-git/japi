package org.comroid.api.func;

import java.util.function.Predicate;

public interface PredicateDuo<A, B> {
    static <A, B> PredicateDuo<A, B> any() {
        return of(any -> true, any -> true);
    }

    static <A, B> PredicateDuo<A, B> of(Predicate<A> aPredicate, Predicate<B> bPredicate) {
        return new PredicateDuo<A, B>() {
            private final Predicate<A> first = aPredicate;
            private final Predicate<B> second = bPredicate;

            @Override
            public boolean testFirst(A a) {
                return first.test(a);
            }

            @Override
            public boolean testSecond(B b) {
                return second.test(b);
            }
        };
    }

    boolean testFirst(A a);

    boolean testSecond(B b);
}
