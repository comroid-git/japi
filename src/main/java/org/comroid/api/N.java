package org.comroid.api;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@UtilityClass
public class N {
    private interface dim {
        interface O0 extends dim {
        }

        interface O1<R> extends O0 {
            @Nullable
            default R getDefaultR() {
                return null;
            }
        }

        interface I0 extends dim {
        }

        interface I1<X> extends I0 {
            @Nullable
            default X getDefaultX() {
                return null;
            }
        }

        interface I2<X, Y> extends I1<X> {
            @Nullable
            default Y getDefaultY() {
                return null;
            }
        }

        interface I3<X, Y, Z> extends I2<X, Y> {
            @Nullable
            default Z getDefaultZ() {
                return null;
            }
        }

        interface I4<X, Y, Z, W> extends I3<X, Y, Z> {
            @Nullable
            default W getDefaultW() {
                return null;
            }
        }
    }

    interface Runnable extends java.lang.Runnable, dim.I0, dim.O0 {
        @Override
        void run();
    }

    interface Supplier {
        @FunctionalInterface
        interface $1<R> extends java.util.function.Supplier<R>, dim.I0, dim.O1<R> {
            @Override
            R get();

            default <O> $1<O> then(final java.util.function.Function<R, O> function) {
                return ()->function.apply(get());
            }
        }
    }

    interface Function {
        @FunctionalInterface
        interface $1<X, R> extends java.util.function.Function<X, R>, dim.I1<X>, dim.O1<R> {
            @Override
            R apply(X x);

            @NotNull
            @Override
            default <O> $1<O, R> compose(@NotNull java.util.function.Function<? super O, ? extends X> before) {
                return o -> apply(before.apply(o));
            }

            default <Y, O> $2<X, Y, O> andThen($2<R, Y, O> after) {
                return (x, y) -> after.apply(apply(x), y);
            }
        }

        @FunctionalInterface
        interface $2<X, Y, R> extends BiFunction<X, Y, R>, $1<X, R>, dim.I2<X, Y>, dim.O1<R> {
            @Override
            R apply(X x, Y y);

            @Override
            default R apply(X x) {
                return apply(x, getDefaultY());
            }

            @NotNull
            @Override
            default <I> $2<I, Y, R> compose(final @NotNull java.util.function.Function<? super I, ? extends X> before) {
                return (i, y) -> apply(before.apply(i), y);
            }

            @NotNull
            @Override
            default <O> $2<X, Y, O> andThen(final @NotNull java.util.function.Function<? super R, ? extends O> after) {
                return (x, y) -> after.apply(apply(x, y));
            }

            default <Z, O> $3<X, Y, Z, O> andThen($3<R, Y, Z, O> after) {
                return (x, y, z) -> after.apply(apply(x, y), y, z);
            }
        }

        @FunctionalInterface
        interface $3<X, Y, Z, R> extends $2<X, Y, R>, dim.I3<X, Y, Z>, dim.O1<R> {
            R apply(X x, Y y, Z z);

            @Override
            default R apply(X x, Y y) {
                return apply(x, y, getDefaultZ());
            }

            @NotNull
            @Override
            default <I> $3<I, Y, Z, R> compose(final @NotNull java.util.function.Function<? super I, ? extends X> before) {
                return (i, y, z) -> apply(before.apply(i), y, z);
            }

            @Override
            @NotNull
            default <O> $3<X, Y, Z, O> andThen(final @NotNull java.util.function.Function<? super R, ? extends O> after) {
                return (x, y, z) -> after.apply(apply(x, y, z));
            }

            default <W, O> $4<X, Y, Z, W, O> andThen($4<R, Y, Z, W, O> after) {
                return (x, y, z, w) -> after.apply(apply(x, y, z), y, z, w);
            }
        }

        @FunctionalInterface
        interface $4<X, Y, Z, W, R> extends $3<X, Y, Z, R>, dim.I4<X, Y, Z, W>, dim.O1<R> {
            R apply(X x, Y y, Z z, W w);

            @Override
            default R apply(X x, Y y, Z z) {
                return apply(x, y, z, getDefaultW());
            }

            @NotNull
            @Override
            default <I> $4<I, Y, Z, W, R> compose(final @NotNull java.util.function.Function<? super I, ? extends X> before) {
                return (x, y, z, w) -> apply(before.apply(x), y, z, w);
            }

            @Override
            @NotNull
            default <O> $4<X, Y, Z, W, O> andThen(final @NotNull java.util.function.Function<? super R, ? extends O> after) {
                return (x, y, z, w) -> after.apply(apply(x, y, z, w));
            }
        }
    }

    interface Consumer {
        @FunctionalInterface
        interface $1<X> extends java.util.function.Consumer<X>, dim.I1<X>, dim.O0 {
            @Override
            void accept(X x);

            @NotNull
            @Override
            default java.util.function.Consumer<X> andThen(@NotNull java.util.function.Consumer<? super X> after) {
                return java.util.function.Consumer.super.andThen(after);
            }

            default <Y> $2<X, Y> andThen(final @NotNull $2<? super X, Y> after) {
                return (x, y) -> {
                    accept(x);
                    after.accept(x, y);
                };
            }
        }

        @FunctionalInterface
        interface $2<X, Y> extends BiConsumer<X, Y>, $1<X>, dim.I2<X, Y>, dim.O0 {
            @Override
            void accept(X x, Y y);

            @Override
            default void accept(X x) {
                accept(x, getDefaultY());
            }

            @NotNull
            @Override
            default BiConsumer<X, Y> andThen(@NotNull BiConsumer<? super X, ? super Y> after) {
                return (x, y) -> {
                    accept(x, y);
                    after.accept(x, y);
                };
            }

            default <Z> $3<X, Y, Z> andThen(final @NotNull $3<? super X, Y, Z> after) {
                return (x, y, z) -> {
                    accept(x, y);
                    after.accept(x, y, z);
                };
            }
        }

        @FunctionalInterface
        interface $3<X, Y, Z> extends $2<X, Y>, dim.I3<X, Y, Z>, dim.O0 {
            void accept(X x, Y y, Z z);

            @Override
            default void accept(X x, Y y) {
                accept(x, y, getDefaultZ());
            }

            @NotNull
            default $3<X, Y, Z> andThen(final @NotNull java.util.function.Consumer<? super X> after) {
                return (x, y, z) -> {
                    accept(x, y, z);
                    after.accept(x);
                };
            }

            @Override
            @NotNull
            default $3<X, Y, Z> andThen(final @NotNull BiConsumer<? super X, ? super Y> after) {
                return (x, y, z) -> {
                    accept(x, y, z);
                    after.accept(x, y);
                };
            }
        }
    }
}
