package org.comroid.api;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

@UtilityClass
public final class NFunction {
    @FunctionalInterface
    public interface In1<X, R> extends Function<X, R> {
        @Override
        R apply(X x);

        @NotNull
        @Override
        default <V> In1<V, R> compose(@NotNull Function<? super V, ? extends X> before) {
            return v -> apply(before.apply(v));
        }
    }

    @FunctionalInterface
    public interface In2<X, Y, R> extends BiFunction<X, Y, R>, In1<X, R> {
        default @Nullable Y getDefaultB() {
            return null;
        }

        @Override
        R apply(X x, @Nullable Y y);

        @Override
        default R apply(X x) {
            return apply(x, getDefaultB());
        }

        @NotNull
        @Override
        default <I> In2<I, @Nullable Y, R> compose(final @NotNull Function<? super I, ? extends X> before) {
            return (i, y) -> apply(before.apply(i), y);
        }

        @NotNull
        @Override
        default <O> In2<X, @Nullable Y, O> andThen(final @NotNull Function<? super R, ? extends O> after) {
            return (x, y) -> after.apply(apply(x, y));
        }
    }

    @FunctionalInterface
    public interface In3<X, Y, Z, R> extends In2<X, Y, R> {
        default @Nullable Y getDefaultB() {
            return null;
        }

        default @Nullable Z getDefaultC() {
            return null;
        }

        R apply(X x, @Nullable Y y, @Nullable Z z);

        @Override
        default R apply(X x, @Nullable Y y) {
            return apply(x, y, getDefaultC());
        }

        @Override
        default R apply(X x) {
            return apply(x, getDefaultB());
        }

        @NotNull
        @Override
        default <I> In3<I, @Nullable Y, @Nullable Z, R> compose(final @NotNull Function<? super I, ? extends X> before) {
            return (i, y, z) -> apply(before.apply(i), y, z);
        }

        @Override
        @NotNull
        default <O> In3<X, @Nullable Y, @Nullable Z, O> andThen(final @NotNull Function<? super R, ? extends O> after) {
            return (x, y, z) -> after.apply(apply(x, y, z));
        }
    }
}
