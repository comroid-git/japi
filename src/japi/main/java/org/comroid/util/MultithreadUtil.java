package org.comroid.util;

import org.comroid.api.Polyfill;
import org.comroid.api.info.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

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

    public static <T> CompletableFuture<T> firstOf(CompletableFuture<?>... futures) {
        final var future = new CompletableFuture<T>();
        for (var each : futures)
            each.thenApply(Polyfill::<T>uncheckedCast)
                    .thenAccept(future::complete)
                    .exceptionally(Polyfill.exceptionLogger(Log.get("firstOf"), Level.FINE, "Error in .firstOf() member"));
        return future;
    }
}
