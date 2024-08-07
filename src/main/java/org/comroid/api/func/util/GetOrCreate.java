package org.comroid.api.func.util;

import lombok.Data;
import lombok.extern.java.Log;
import org.comroid.api.func.exc.ThrowingFunction;
import org.comroid.api.func.exc.ThrowingSupplier;
import org.comroid.api.info.Constraint;
import org.comroid.api.java.StackTraceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@Log
@Data
public class GetOrCreate<T, B> extends Almost<T, B> {
    private final @Nullable ThrowingSupplier<@Nullable T, Throwable>             get;
    private final @NotNull  ThrowingSupplier<@NotNull B, Throwable>              create;
    private final @NotNull  ThrowingFunction<@NotNull B, @Nullable T, Throwable> build;
    private final @NotNull Function<@Nullable T, @Nullable T> finalize;
    private final          List<Consumer<T>>                  completionCallbacks = new ArrayList<>();
    private @Nullable       Function<T, T>                                       updateOriginal;
    private @Nullable       Function<Throwable, @NotNull T>                      exceptionHandler;

    @Override
    public @NotNull T complete(@Nullable Consumer<B> modifier) {
        int c = 0;
        B builder;
        T result;
        try {
            if (get != null && (result = get.get()) != null)
                return updateOriginal == null ? result : updateOriginal.apply(result);
            c++; // 1 - passed original

            builder = create.get();
            c++; // 2 - passed origin

            if (modifier != null)
                modifier.accept(builder);
            c++; // 3 - passed modifier

            result = build.apply(builder);
            c++; // 4 - passed finalize
        } catch (Throwable t) {
            var stage = switch (c) {
                case 0 -> "trying to obtain original";
                case 1 -> "initializing created value";
                case 2 -> "modifying created value";
                case 3 -> "finalizing created value";
                default -> throw new IllegalStateException("Unexpected value: " + c);
            };
            var msg = " from an exception that occurred when " + stage;

            if (exceptionHandler == null)
                throw new RuntimeException("No exception handler found; cannot recover" + msg, t);
            result = exceptionHandler.apply(t);

            // counter cannot be >3 because 4 = success
            Constraint.Range.inside(0, 3, c, "stage counter").run();

            log.fine("Recovered" + msg);
            log.finer(StackTraceUtils.toString(t));
        }
        if (result != null)
            result = finalize.apply(result);
        if (result != null)
            for (Consumer<T> callback : completionCallbacks)
                callback.accept(result);
        return Objects.requireNonNull(result);
    }

    public GetOrCreate<T, B> addCompletionCallback(Consumer<T> callback) {
        completionCallbacks.add(callback);
        return this;
    }
}
