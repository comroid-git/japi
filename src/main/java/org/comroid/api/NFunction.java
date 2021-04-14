package org.comroid.api;

import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class NFunction {
    private NFunction() {
        throw new UnsupportedOperationException();
    }

    @FunctionalInterface
    public interface In1<A, R> extends Function<A, R> {
        @Override
        R apply(A a);

        @NotNull
        @Override
        default <V> In1<V, R> compose(@NotNull Function<? super V, ? extends A> before) {
            return v -> apply(before.apply(v));
        }
    }

    @FunctionalInterface
    public interface In2<A, B, R> extends BiFunction<A, B, R>, In1<A, R> {
        default B getDefaultB() {
            return null;
        }

        @Override
        R apply(A a, B b);

        @Override
        default R apply(A a) {
            return apply(a, getDefaultB());
        }

        @NotNull
        @Override
        default <V> In2<V, B, R> compose(@NotNull Function<? super V, ? extends A> before) {
            return (v, b) -> apply(before.apply(v), b);
        }

        @NotNull
        @Override
        default <V> In2<A, B, V> andThen(final @NotNull Function<? super R, ? extends V> after) {
            return (a, b) -> after.apply(apply(a, b));
        }
    }

    @FunctionalInterface
    public interface In3<A, B, C, R> extends In2<A, B, R> {
        default B getDefaultB() {
            return null;
        }

        default C getDefaultC() {
            return null;
        }

        R apply(A a, B b, C c);

        @Override
        default R apply(A a, B b) {
            return apply(a, b, getDefaultC());
        }

        @Override
        default R apply(A a) {
            return apply(a, getDefaultB());
        }

        @NotNull
        @Override
        default <V> In3<V, B, C, R> compose(@NotNull Function<? super V, ? extends A> before) {
            return (v, b, c) -> apply(before.apply(v), b, c);
        }

        @Override
        @NotNull
        default <V> In3<A, B, C, V> andThen(final @NotNull Function<? super R, ? extends V> after) {
            return (a, b, c) -> after.apply(apply(a, b, c));
        }
    }
}
