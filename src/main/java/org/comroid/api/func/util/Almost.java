package org.comroid.api.func.util;

import lombok.extern.slf4j.Slf4j;
import org.comroid.api.func.ext.Wrap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@Slf4j
public abstract class Almost<T, B> implements Wrap<T> {
    public T get() {
        try {
            return complete(null);
        } catch (Throwable t) {
            log.warn("Could not finalize object", t);
            return null;
        }
    }

    @NotNull
    public T complete(@Nullable Consumer<B> modifier) {return complete(modifier, null);}

    @NotNull
    public abstract T complete(@Nullable Consumer<B> modifier, @Nullable Consumer<T> finalizer);
}
