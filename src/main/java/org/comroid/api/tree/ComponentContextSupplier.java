package org.comroid.api.tree;

import org.comroid.api.func.ext.Wrap;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface ComponentContextSupplier {
    <T extends Component> Stream<T> components(@Nullable Class<? super T> type);

    <T extends Component> Wrap<T> component(@Nullable Class<? super T> type);
}
