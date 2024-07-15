package org.comroid.api.func.util;

import lombok.Data;
import lombok.extern.java.Log;
import org.comroid.api.func.exc.ThrowingConsumer;
import org.comroid.api.func.exc.ThrowingSupplier;
import org.comroid.api.info.Constraint;
import org.comroid.api.java.StackTraceUtils;
import org.comroid.exception.RethrownException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

@Log
@Data
public class AlmostComplete<T> extends Almost<T, T> {
    public static <T> AlmostComplete<T> of(final @NotNull T value) {
        return new AlmostComplete<>(() -> value, null);
    }

    private final @NotNull ThrowingSupplier<@NotNull T, Throwable> origin;
    private final @Nullable ThrowingConsumer<@NotNull T, Throwable> finalize;
    private @Nullable      Function<Throwable, @Nullable T>        exceptionHandler;

    @Override
    public @NotNull T complete(@Nullable Consumer<T> modifier) {
        int c  = 0;
        T   it = null;
        try {
            it = origin.get();
            c++; // 1 - passed origin
            if (modifier != null)
                modifier.accept(it);
            c++; // 2 - passed modifier
            if (finalize != null)
                finalize.accept(it);
            c++; // 3 - passed finalize
        } catch (Throwable t) {
            if (exceptionHandler != null)
                it = exceptionHandler.apply(t);

            // counter cannot be >2 because 3 = success
            Constraint.Range.inside(0, 2, c, "stage counter").run();

            var stage = switch (c) {
                case 0 -> "initialization";
                case 1 -> "modification";
                case 2 -> "finalizing";
                default -> throw new IllegalStateException("Unexpected value: " + c);
            };
            if (it == null)
                throw new RethrownException(t);
            log.fine("Recovered from an exception that occurred during " + stage);
            log.finer(StackTraceUtils.toString(t));
        }
        return it;
    }
}
