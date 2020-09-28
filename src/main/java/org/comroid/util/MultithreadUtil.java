package org.comroid.util;

import java.util.concurrent.*;

public final class MultithreadUtil {
    public static <T> CompletableFuture<T> submitQuickTask(
            ExecutorService executorService, Callable<T> task
    ) {
        final CompletableFuture<T> future = new CompletableFuture<>();

        executorService.submit(() -> {
            T result = null;

            try {
                result = task.call();
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                future.complete(result);
            }
        });

        return future;
    }

    public static CompletableFuture<Void> futureAfter(long time, TimeUnit unit) {
        return futureAfter(null, time, unit);
    }

    public static <T> CompletableFuture<T> futureAfter(T value, long time, TimeUnit unit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(TimeUnit.MILLISECONDS.convert(time, unit));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return value;
        });
    }
}
