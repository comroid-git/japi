package org.comroid.api.info;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.StandardException;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Streams;
import org.comroid.api.java.StackTraceUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.IntStream.range;
import static org.comroid.api.func.util.Streams.intString;

@Log
@UtilityClass
public class Constraint {
    @UtilityClass
    public class Type {
        public API anyOf(Object it, String nameof, Class<?>... types) {
            final var tt = it instanceof Class<?>;
            return decide(Arrays.stream(types)
                    .anyMatch(x -> (tt && x.isAssignableFrom((Class<?>) it)) || x.isInstance(it)))
                    .setConstraint("Type comparison")
                    .setTypeof(it.getClass())
                    .setNameof(nameof)
                    .setShouldBe("any of types")
                    .setExpected(Arrays.toString(types));
        }

        public API noneOf(Object it, String nameof, Class<?>... types) {
            final var tt = it instanceof Class<?>;
            return decide(Arrays.stream(types)
                    .noneMatch(x -> (tt && x.isAssignableFrom((Class<?>) it)) || x.isInstance(it)))
                    .setConstraint("Type comparison")
                    .setTypeof(it.getClass())
                    .setNameof(nameof)
                    .setShouldBe("none of types")
                    .setExpected(Arrays.toString(types));
        }
    }

    @UtilityClass
    public class Range {
        public API inside(double xIncl, double yIncl, double actual, String nameof) {
            return combine(Length.min(xIncl, actual, nameof + " range lower end"),
                    Length.max(yIncl, actual, nameof + " range upper end"))
                    .setNameof("range (%f..%f)".formatted(xIncl, yIncl));
        }
    }

    @UtilityClass
    public class Length {
        public API min(double min, Object target, String nameof) {
            int len = Integer.MIN_VALUE;
            if (target.getClass().isArray())
                len = ((Object[]) target).length;
            else if (target instanceof Collection<?>)
                len = ((Collection<?>) target).size();
            else if (target instanceof Map<?, ?>)
                len = ((Map<?, ?>) target).size();
            return decide(len >= min)
                    .setConstraint("minLength")
                    .setTypeof(target.getClass())
                    .setNameof(nameof)
                    .setActual(len)
                    .setShouldBe("at least")
                    .setExpected(min);
        }

        public API max(double max, Object target, String nameof) {
            int len = Integer.MAX_VALUE;
            if (target.getClass().isArray())
                len = ((Object[]) target).length;
            else if (target instanceof Collection<?>)
                len = ((Collection<?>) target).size();
            else if (target instanceof Map<?, ?>)
                len = ((Map<?, ?>) target).size();
            else if (target instanceof Iterable<?>)
                len = (int) Streams.of((Iterable<?>) target).count();
            else if (target instanceof Spliterator<?>)
                len = (int) Streams.of((Spliterator<?>) target).count();
            return decide(len <= max)
                    .setConstraint("maxLength")
                    .setTypeof(target.getClass())
                    .setNameof(nameof)
                    .setActual(len)
                    .setShouldBe("at most")
                    .setExpected(max);
        }
    }

    public API anyOf(Object actual, String nameof, Object... expected) {
        return new API(() -> Arrays.asList(expected).contains(actual))
                .setConstraint("anyOf")
                .setTypeof(actual.getClass())
                .setNameof(nameof)
                .setActual(actual)
                .setShouldBe("any of " + expected.getClass().getSimpleName())
                .setExpected(expected);
    }

    public API equals(Object actual, Object expected, String nameof) {
        return new API(() -> Objects.equals(actual, expected))
                .setConstraint("equals")
                .setTypeof(actual.getClass())
                .setNameof(nameof)
                .setActual(actual)
                .setShouldBe("equal to " + expected.getClass().getSimpleName())
                .setExpected(expected);
    }

    @Contract("false, _ -> fail")
    public API decide(boolean pass, String nameof) {
        return decide(pass)
                .setConstraint("check")
                .setNameof(nameof)
                .setShouldBe("is")
                .setExpected("true");
    }

    @Contract("null, _ -> new; _, _ -> fail")
    public API isNull(Object it, String nameof) {
        return new API(() -> it == null)
                .setConstraint("isNull")
                .setNameof(nameof)
                .setShouldBe("is");
    }

    @Contract("null, _ -> fail")
    public API notNull(Object it, String nameof) {
        return new API(() -> it != null)
                .setConstraint("notNull")
                .setNameof(nameof)
                .setShouldBe("not");
    }

    @Contract("false -> fail")
    private API decide(boolean pass) {
        return pass ? pass() : fail();
    }

    public API combine(API... apis) {
        Length.min(1, apis, "apis").run();
        if (apis.length == 1) return apis[0];
        var base = apis[0];
        for (var i = 1; i < apis.length; i++) {
            if (API.DefaultHandler.equals(base.handler) && !API.DefaultHandler.equals(apis[i].handler))
                base.handler = apis[i].handler;
            if (API.DefaultConstraint.equals(base.constraint) && !API.DefaultConstraint.equals(apis[i].constraint))
                base.constraint = apis[i].constraint;
            if (API.DefaultTypeof.equals(base.typeof) && !API.DefaultTypeof.equals(apis[i].typeof))
                base.typeof = apis[i].typeof;
            if (API.DefaultNameof.equals(base.nameof) && !API.DefaultNameof.equals(apis[i].nameof))
                base.nameof = apis[i].nameof;
            if (API.DefaultActual.equals(base.actual) && !API.DefaultActual.equals(apis[i].actual))
                base.actual = apis[i].actual;
            if (API.DefaultShouldBe.equals(base.shouldBe) && !API.DefaultShouldBe.equals(apis[i].shouldBe))
                base.shouldBe = apis[i].shouldBe;
            if (API.DefaultExpected.equals(base.expected) && !API.DefaultExpected.equals(apis[i].expected))
                base.expected = apis[i].expected;
        }
        return base;
    }

    @Contract("-> new")
    public API pass() {
        return new API(() -> true);
    }

    @Contract("-> fail")
    public API fail() {
        return new API(() -> false);
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static final class API {
        public static final Function<UnmetError, @Nullable Object> ThrowingHandler = e -> {
            throw e;
        };
        public static final Function<UnmetError, @Nullable Object> LoggingHandler = e -> {
            log.warning(StackTraceUtils.toString(e));
            return null;
        };
        public static Function<UnmetError, @Nullable Object> DefaultHandler = ThrowingHandler;
        public static String DefaultConstraint = "<unnamed>";
        public static Class<?> DefaultTypeof = Void.class;
        public static String DefaultNameof = "<unnamed>";
        public static String DefaultCallLocation = intString(range(0, " in call to ".length()).map($ -> '\b'));
        public static Object DefaultActual = "\b";
        public static String DefaultShouldBe = intString(range(0, "; should be ".length()).map($ -> '\b'));
        public static Object DefaultExpected = "\b";
        public static String DefaultHint = "\b";

        @NotNull BooleanSupplier test;
        @NotNull Function<UnmetError, @Nullable Object> handler = DefaultHandler;
        @NotNull String constraint = DefaultConstraint;
        @NotNull Class<?> typeof = DefaultTypeof;
        @NotNull String nameof = DefaultNameof;
        @NotNull String callLocation = DefaultCallLocation;
        @NotNull Object actual = DefaultActual;
        @NotNull String shouldBe = DefaultShouldBe;
        @NotNull Object expected = DefaultExpected;
        @NotNull String hint = DefaultHint;
        @Nullable String messageOverride = null;

        @Contract(mutates = "this")
        public API invert() {
            final var wrap = test;
            test = () -> !wrap.getAsBoolean();
            shouldBe = DefaultShouldBe + "; should not be ";
            return this;
        }

        public <T> Wrap<T> handle(@NotNull Supplier<T> success, @Nullable Function<UnmetError, @Nullable T> failure) {
            return () -> {
                T result = null;
                if (test.getAsBoolean())
                    result = success.get();
                else if (failure != null) {
                    var txt = err(constraint, typeof.getCanonicalName(), nameof, callLocation, actual, shouldBe, expected, hint);
                    var err = new UnmetError(txt);
                    result = failure.apply(err);
                    if (result == null && !err.isResolved()) {
                        var fix = handler.apply(err);
                        if (fix == null)
                            log.warning("Recovering from unmet Constraint " + this + " failed");
                    }
                }
                return result;
            };
        }

        public Wrap<Object> handle() {
            return handle(Object::new, $ -> null);
        }

        public void run() {
            handle().get();
        }

        @Override
        public String toString() {
            return err(constraint, typeof.getSimpleName(), nameof, callLocation, actual, shouldBe, expected, hint);
        }

        private String err(
                String constraint,
                String typeof,
                String nameof,
                String callLocation,
                Object actual,
                String shouldBeVerb,
                Object expected,
                @Nullable String hint
        ) {
            if (messageOverride != null)
                return messageOverride;
            return "Unmet %s constraint for argument %s %s in call to %s;\n%s should be %s %s\n%s"
                    .formatted(constraint, typeof, nameof, callLocation, actual, shouldBeVerb, expected, hint);
        }
    }

    @Data
    @StandardException
    public final class UnmetError extends IllegalArgumentException {
        private boolean resolved = false;

        public void cancel() {
            resolved = true;
        }

        @Override
        public String toString() {
            return getMessage();
        }
    }
}
