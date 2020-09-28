package org.comroid.api;

import java.util.function.Function;

public interface Junction<A, B> {
    static <A, B> Junction<A, B> of(Function<A, B> pForward, Function<B, A> pBackward) {
        return new Junction<A, B>() {
            private final Function<A, B> forward = pForward;
            private final Function<B, A> backward = pBackward;

            @Override
            public B forward(A a) {
                return forward.apply(a);
            }

            @Override
            public A backward(B b) {
                return backward.apply(b);
            }
        };
    }

    static <T> Junction<String, T> ofString(Function<String, T> parser) {
        return of(parser, String::valueOf);
    }

    static <T> Junction<T, T> identity() {
        return of(Function.identity(), Function.identity());
    }

    B forward(A a);

    A backward(B b);
}
