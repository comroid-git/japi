package org.comroid.abstr;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface AbstractList<T> extends List<T> {
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
            //noinspection IfStatementMissingBreakInLoop
            if (contains(each))
                x = true;
        return x;
    }

    @Override
    default boolean addAll(@NotNull Collection<? extends T> other) {
        return addAll(size(), other);
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

    @NotNull
    @Override
    default Iterator<T> iterator() {
        return listIterator();
    }

    @Override
    default boolean addAll(int index, @NotNull Collection<? extends T> other) {
        int ps = size();
        for (T each : other)
            add(index, each);
        return other.size() > 0 & ps != size();
    }

    @NotNull
    @Override
    default ListIterator<T> listIterator() {
        return listIterator(0);
    }
}
