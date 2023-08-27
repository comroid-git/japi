package org.comroid.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface OptionalFunction<I, O> extends N.Function.$1<I,O> {
    @Override
    @Nullable
    default O apply(@Nullable I in) {
        return in != null ? wrap(in) : null;
    }

    @Nullable O wrap(@NotNull I in);
}
