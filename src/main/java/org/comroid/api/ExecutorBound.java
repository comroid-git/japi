package org.comroid.api;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public interface ExecutorBound extends Context.Member {
    Executor getExecutor();

    @Override
    @NotNull
    default Executor getFromContext() {
        return getExecutor();
    }
}
