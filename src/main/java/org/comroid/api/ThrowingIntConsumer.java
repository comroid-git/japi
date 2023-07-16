package org.comroid.api;

import lombok.SneakyThrows;

import java.util.function.IntConsumer;

public interface ThrowingIntConsumer<T extends Throwable> {
    void accept(int value) throws T;

    static IntConsumer sneaky(final ThrowingIntConsumer<?> consumer) {
        return x -> handleSneaky(consumer, x);
    }

    @SneakyThrows
    private static void handleSneaky(ThrowingIntConsumer<?> consumer, int x) {
        consumer.accept(x);
    }
}
