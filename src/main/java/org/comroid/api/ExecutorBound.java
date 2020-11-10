package org.comroid.api;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

@Deprecated
public interface ExecutorBound extends ContextualTypeProvider<Executor> {
    @Deprecated
    Executor getExecutor();

    @Override
    @NotNull
    default Executor getFromContext() {
        return getExecutor();
    }
}
