package org.comroid.api;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.*;
import java.util.stream.Stream;

@UtilityClass
public class N {
    private interface dim {
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

        interface O0 extends dim {
        }

        interface O1<R> extends O0 {
            @Nullable
            default R getDefaultR() {
                return null;
            }
        }
    }

    @FunctionalInterface
    public interface Runnable extends java.lang.Runnable, dim.I0, dim.O0 {
        @Override
        void run();
    }

    @FunctionalInterface
    public interface Supplier<R> extends java.util.function.Supplier<R>, dim.I0, dim.O1<R> {
        @Override
        R get();

        default <O> Supplier<O> then(final java.util.function.Function<R, O> function) {
            return () -> function.apply(get());
        }
    }

    public interface Function {
        @FunctionalInterface
        interface $1<X, R> extends Function, java.util.function.Function<X, R>, dim.I1<X>, dim.O1<R> {
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

    public interface Consumer {
        @FunctionalInterface
        interface $1<X> extends Consumer, java.util.function.Consumer<X>, dim.I1<X>, dim.O0 {
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

        @FunctionalInterface
        interface $4<X, Y, Z, W> extends $3<X, Y, Z>, dim.I4<X, Y, Z, W>, dim.O0 {
            void accept(X x, Y y, Z z);

            @Override
            default void accept(X x, Y y) {
                accept(x, y, getDefaultZ());
            }

            @NotNull
            default $4<X, Y, Z, W> andThen(final @NotNull java.util.function.Consumer<? super X> after) {
                return (x, y, z) -> {
                    accept(x, y, z);
                    after.accept(x);
                };
            }

            @Override
            @NotNull
            default $4<X, Y, Z, W> andThen(final @NotNull BiConsumer<? super X, ? super Y> after) {
                return (x, y, z) -> {
                    accept(x, y, z);
                    after.accept(x, y);
                };
            }
        }
    }

    /*
    @With
    @SuppressWarnings("ALL")
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
    public final class Adapter<I extends dim.I0,O extends dim.O0,R,X,Y,Z,W, D> {
        //region fields
        @Nullable I i = null;
        @Nullable O o = null;
        @Nullable R r = null;
        @Nullable X x = null;
        @Nullable Y y = null;
        @Nullable Z z = null;
        @Nullable W w = null;
        //endregion

        //region starters
        public static <D extends dim.I0          & dim.O0   >            Adapter<dim.I0          ,dim.O0,    Void, Object,Object,Object,Object,D>   R0() {}
        public static <D extends dim.I0          & dim.O1<R>, R>         Adapter<dim.I0          ,dim.O1<R>, R, Object,Object,Object,Object, D>     S1() {}
        public static <D extends dim.I1<X>       & dim.O0   , X>         Adapter<dim.I1<X>       ,dim.O0,    Void, X,Object,Object,Object, D>  C1() {}
        public static <D extends dim.I2<X,Y>     & dim.O0   , X,Y>       Adapter<dim.I2<X,Y>     ,dim.O0,    Void, X,Y,Object,Object, D>  C2() {}
        public static <D extends dim.I3<X,Y,Z>   & dim.O0   , X,Y,Z>     Adapter<dim.I3<X,Y,Z>   ,dim.O0,    Void, X,Y,Z,Object, D>  C3() {}
        public static <D extends dim.I4<X,Y,Z,W> & dim.O0   , X,Y,Z,W>   Adapter<dim.I4<X,Y,Z,W> ,dim.O0,    Void, X,Y,Z,W, D>  C4() {}
        public static <D extends dim.I1<X>       & dim.O1<R>, R,X>       Adapter<dim.I1<X>       ,dim.O1<R>, R, X,Object,Object,Object, D>     F1() {}
        public static <D extends dim.I2<X,Y>     & dim.O1<R>, R,X,Y>     Adapter<dim.I2<X,Y>     ,dim.O1<R>, R, X,Y,Object,Object, D>     F2() {}
        public static <D extends dim.I3<X,Y,Z>   & dim.O1<R>, R,X,Y,Z>   Adapter<dim.I3<X,Y,Z>   ,dim.O1<R>, R, X,Y,Z,Object, D>     F3() {}
        public static <D extends dim.I4<X,Y,Z,W> & dim.O1<R>, R,X,Y,Z,W> Adapter<dim.I4<X,Y,Z,W> ,dim.O1<R>, R, X,Y,Z,W, D>     F4() {}
        //endregion

        //region parameters
        public <NR> Adapter<I,O,NR,X,Y,Z,W, ?> r() {}
        public <NX> Adapter<I,O,R,NX,Y,Z,W, ?> x() {}
        public <NY> Adapter<I,O,R,X,NY,Z,W, ?> y() {}
        public <NZ> Adapter<I,O,R,X,Y,NZ,W, ?> z() {}
        public <NW> Adapter<I,O,R,X,Y,Z,NW, ?> w() {}

        public <NR> Adapter<I,O,NR,X,Y,Z,W, ?> r(NR nr) {}
        public <NX> Adapter<I,O,R,NX,Y,Z,W, ?> x(NX nx) {}
        public <NY> Adapter<I,O,R,X,NY,Z,W, ?> y(NY ny) {}
        public <NZ> Adapter<I,O,R,X,Y,NZ,W, ?> z(NZ nz) {}
        public <NW> Adapter<I,O,R,X,Y,Z,NW, ?> w(NW nw) {}

        public <NR extends R> Adapter<I,O,NR,X,Y,Z,W, ?> rO(Optional<NR> nr) {}
        public <NX extends X> Adapter<I,O,R,NX,Y,Z,W, ?> xO(Optional<NX> nx) {}
        public <NY extends Y> Adapter<I,O,R,X,NY,Z,W, ?> yO(Optional<NY> ny) {}
        public <NZ extends Z> Adapter<I,O,R,X,Y,NZ,W, ?> zO(Optional<NZ> nz) {}
        public <NW extends W> Adapter<I,O,R,X,Y,Z,NW, ?> wO(Optional<NW> nw) {}

        public <NR> Adapter<I,O,NR,X,Y,Z,W, ?> rS(Stream<NR> nr) {}
        public <NX> Adapter<I,O,R,NX,Y,Z,W, ?> xS(Stream<NX> nx) {}
        public <NY> Adapter<I,O,R,X,NY,Z,W, ?> yS(Stream<NY> ny) {}
        public <NZ> Adapter<I,O,R,X,Y,NZ,W, ?> zS(Stream<NZ> nz) {}
        public <NW> Adapter<I,O,R,X,Y,Z,NW, ?> wS(Stream<NW> nw) {}

        public <NR> Adapter<I,O,NR,X,Y,Z,W, ?> rR(java.util.function.Supplier<NR> nr) {}
        public <NX> Adapter<I,O,R,NX,Y,Z,W, ?> xR(java.util.function.Supplier<NX> nx) {}
        public <NY> Adapter<I,O,R,X,NY,Z,W, ?> yR(java.util.function.Supplier<NY> ny) {}
        public <NZ> Adapter<I,O,R,X,Y,NZ,W, ?> zR(java.util.function.Supplier<NZ> nz) {}
        public <NW> Adapter<I,O,R,X,Y,Z,NW, ?> wR(java.util.function.Supplier<NW> nw) {}

        public <NR> Adapter<I,O,NR,X,Y,Z,W, ?> rF(Provider<NR> nr) {}
        public <NX> Adapter<I,O,R,NX,Y,Z,W, ?> xF(Provider<NX> nx) {}
        public <NY> Adapter<I,O,R,X,NY,Z,W, ?> yF(Provider<NY> ny) {}
        public <NZ> Adapter<I,O,R,X,Y,NZ,W, ?> zF(Provider<NZ> nz) {}
        public <NW> Adapter<I,O,R,X,Y,Z,NW, ?> wF(Provider<NW> nw) {}
        //endregion

        //region delegates
        public D d(Runnable delegate) {}
        public D d(java.util.function.Consumer<X> delegate) {}
        public D d(BiConsumer<X,Y> delegate) {}
        public D d(Consumer.$3<X,Y,Z> delegate) {}
        public D d(Consumer.$4<X,Y,Z,W> delegate) {}
        public D d(java.util.function.Supplier<R> delegate) {}
        public D d(java.util.function.Function<X,R> delegate) {}
        public D d(BiFunction<X,Y,R> delegate) {}
        public D d(Function.$3<X,Y,Z,R> delegate) {}
        public D d(Function.$4<X,Y,Z,W,R> delegate) {}

        public void $(Runnable                         delegate) {}
        public void $(java.util.function.Consumer<X>   delegate) {}
        public void $(BiConsumer<X,Y>                  delegate) {}
        public void $(Consumer.$3<X,Y,Z>               delegate) {}
        public void $(Consumer.$4<X,Y,Z,W>             delegate) {}
        public R    $(java.util.function.Supplier<R>   delegate) {}
        public R    $(java.util.function.Function<X,R> delegate) {}
        public R    $(BiFunction<X,Y,R>                delegate) {}
        public R    $(Function.$3<X,Y,Z,R>             delegate) {}
        public R    $(Function.$4<X,Y,Z,W,R>           delegate) {}
        //endregion
    }
     */
}
