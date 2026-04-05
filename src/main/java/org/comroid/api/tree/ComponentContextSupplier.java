package org.comroid.api.tree;

import org.comroid.api.func.ext.Wrap;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface ComponentContextSupplier {
    <T> Stream<T> components(@Nullable Class<? super T> type);

    <T> Wrap<T> component(@Nullable Class<? super T> type);
}
