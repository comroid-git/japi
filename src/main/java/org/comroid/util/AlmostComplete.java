package org.comroid.util;

import lombok.Data;
import org.comroid.api.SupplierX;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Data
public class AlmostComplete<T> implements SupplierX<T> {
    private final @NotNull Supplier<T> origin;
    private final @Nullable Consumer<T> finalize;

    public T complete(@Nullable Consumer<T> modifier) {
        var it = origin.get();
        if (modifier != null)
            modifier.accept(it);
        if (finalize != null)
            finalize.accept(it);
        return it;
    }

    @Override
    public final @Nullable T get() {
        return complete(null);
    }

    public static <T> AlmostComplete<T> of(final @NotNull T value) {
        return new AlmostComplete<>(()->value, null);
    }
}
