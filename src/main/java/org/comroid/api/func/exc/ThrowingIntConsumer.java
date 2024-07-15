package org.comroid.api.func.exc;

import lombok.SneakyThrows;

import java.util.function.IntConsumer;

public interface ThrowingIntConsumer<T extends Throwable> {
    static IntConsumer sneaky(final ThrowingIntConsumer<?> consumer) {
        return x -> handleSneaky(consumer, x);
    }

    @SneakyThrows
    private static void handleSneaky(ThrowingIntConsumer<?> consumer, int x) {
        consumer.accept(x);
    }

    void accept(int value) throws T;
}
