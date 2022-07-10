package org.comroid.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class FallbackUtil {
    private FallbackUtil() {
        throw new AbstractMethodError();
    }

    @SafeVarargs
    public static <T> T fallback(@Nullable T from, Supplier<@NotNull T> to, Predicate<? super T>... ifAny) {
        return Arrays.stream(ifAny).anyMatch(x -> x.test(from)) ? to.get() : from;
    }
}
