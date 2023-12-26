package org.comroid.api.abstr;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

public interface AbstractCollection<T> extends Collection<T> {
    @Override
    default boolean isEmpty() {
        return size() == 0;
    }

    @NotNull
    @Override
    default Object[] toArray() {
        //noinspection SimplifyStreamApiCallChains
        return stream().toArray();
    }

    @NotNull
    @Override
    default <T1> T1[] toArray(@NotNull T1[] a) {
        //noinspection SuspiciousToArrayCall
        return stream().toArray(l -> Arrays.copyOf(a, l));
    }

    @Override
    default boolean containsAll(@NotNull Collection<?> other) {
        boolean x = false;
        for (Object each : other)
            if (contains(each))
                x = true;
        return x;
    }

    @Override
    default boolean addAll(@NotNull Collection<? extends T> other) {
        boolean x = false;
        for (T each : other)
            if (add(each))
                x = true;
        return x;
    }

    @Override
    default boolean removeAll(@NotNull Collection<?> other) {
        boolean x = false;
        for (Object each : other)
            if (remove(each))
                x = true;
        return x;
    }

    @Override
    default boolean retainAll(@NotNull Collection<?> other) {
        boolean x = false;
        for (T each : this) {
            if (!other.contains(each) && remove(each))
                x = true;
        }
        return x;
    }
}
