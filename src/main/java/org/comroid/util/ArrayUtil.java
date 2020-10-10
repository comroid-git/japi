package org.comroid.util;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public final class ArrayUtil {
    @SafeVarargs
    public static <T> T[] insert(T[] original, int atIndex, T... insert) {
        final T[] yield = Arrays.copyOf(original, original.length + insert.length);

        for (int i, f, o = i = f = 0; i < yield.length; i++) {
            if (i >= atIndex && o < insert.length) {
                yield[i] = insert[o++];
            } else {
                yield[i] = original[f++];
            }
        }

        return yield;
    }

    public static <T> boolean contains(T[] array, final T other, BiPredicate<T,T> tester) {
        return Stream.of(array).anyMatch(e -> tester.test(e, other));
    }
}
