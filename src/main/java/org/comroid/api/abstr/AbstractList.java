package org.comroid.api.abstr;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public interface AbstractList<T> extends List<T> {
    @Override
    default boolean isEmpty() {
        return size() == 0;
    }

    @Override
    default boolean contains(Object o) {
        ListIterator<T> iterator = listIterator();
        while (iterator().hasNext())
            if (iterator.next().equals(o))
                return true;
        return false;
    }

    @NotNull
    @Override
    default Iterator<T> iterator() {
        return listIterator();
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
    default boolean addAll(int index, @NotNull Collection<? extends T> other) {
        int ps = size();
        int c  = 0;
        for (T each : other)
            add(index + c++, each);
        return other.size() > 0 & ps != size();
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

    @Override
    default int indexOf(Object o) {
        ListIterator<T> iterator = listIterator();
        while (iterator.hasNext())
            if (iterator.next().equals(o))
                return iterator.previousIndex();
        return -1;
    }

    @Override
    default int lastIndexOf(Object o) {
        ListIterator<T> iterator = listIterator();
        int yield = -1;
        while (iterator.hasNext())
            if (iterator.next().equals(o))
                yield = iterator.previousIndex();
        return yield;
    }

    @NotNull
    @Override
    default ListIterator<T> listIterator() {
        return listIterator(0);
    }
}
