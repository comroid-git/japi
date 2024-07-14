package org.comroid.api.func.util;

import lombok.Data;
import lombok.extern.java.Log;
import org.comroid.api.func.exc.ThrowingFunction;
import org.comroid.api.func.exc.ThrowingSupplier;
import org.comroid.api.info.Constraint;
import org.comroid.api.java.StackTraceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@Log
@Data
public class GetOrCreate<T, B> extends Almost<T, B> {
    private final @Nullable ThrowingSupplier<@Nullable T, Throwable> get;
    private final @NotNull ThrowingSupplier<@NotNull B, Throwable> create;
    private final @NotNull ThrowingFunction<@NotNull B, @Nullable T, Throwable> build;
    private final @NotNull Consumer<@Nullable T> finalize;
    private @Nullable Function<T, T> updateOriginal;
    private @Nullable Function<Throwable, @NotNull T> exceptionHandler;

    @Override
    public @NotNull T complete(@Nullable Consumer<B> modifier) {
        int c = 0;
        B builder;
        T result;
        try {
            if (get != null && (result = get.get()) != null)
                return updateOriginal == null ? result : updateOriginal.apply(result);
            builder = create.get();
            c++; // 1 - passed origin
            if (modifier != null)
                modifier.accept(builder);
            c++; // 2 - passed modifier
            result = build.apply(builder);
            c++; // 3 - passed finalize
        } catch (Throwable t) {
            if (exceptionHandler == null)
                throw new RuntimeException("No exception handler found; cannot recover", t);
            result = exceptionHandler.apply(t);

            // counter cannot be >2 because 3 = success
            Constraint.Range.inside(0, 2, c, "stage counter").run();

            var stage = switch (c) {
                case 0 -> "initialization";
                case 1 -> "modification";
                case 2 -> "finalizing";
                default -> throw new IllegalStateException("Unexpected value: " + c);
            };
            log.fine("Recovered from an exception that occurred during " + stage);
            log.finer(StackTraceUtils.toString(t));
        }
        if (result != null)
            finalize.accept(result);
        return Objects.requireNonNull(result);
    }
}
