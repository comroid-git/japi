package org.comroid.api.func.util;

import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;
import org.comroid.api.Polyfill;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static java.util.concurrent.CompletableFuture.*;

@Log
@Value
public class Ratelimit<T, Acc> {
    public static <T, Acc> CompletableFuture<T> run(
            Acc value,
            Duration cooldown,
            AtomicReference<CompletableFuture<@Nullable T>> source,
            BiFunction<@NotNull T, Queue<@NotNull Acc>, CompletableFuture<@Nullable T>> task
    ) {
        return Cache.get(task.getClass(), () -> new Ratelimit<>(cooldown, source, task)).push(value);
    }

    @NotNull Duration                                        cooldown;
    @NotNull AtomicReference<CompletableFuture<@Nullable T>> source;
    @NotNull BiFunction<T, Queue<Acc>, CompletableFuture<T>> accumulator;
    @NotNull Queue<Acc>                                      queue = new LinkedBlockingQueue<>();
    Object lock = Polyfill.selfawareObject();
    @NonFinal
    @Nullable CompletableFuture<T> result;
    @NonFinal
    Instant last = Instant.EPOCH;

    public Ratelimit(
            @NotNull Duration cooldown,
            @NotNull AtomicReference<CompletableFuture<@Nullable T>> source,
            @NotNull BiFunction<T, Queue<Acc>, CompletableFuture<T>> accumulator
    ) {
        this.cooldown    = cooldown;
        this.source      = source;
        this.accumulator = accumulator;
    }

    @SneakyThrows
    public synchronized CompletableFuture<T> push(Acc value) {
        synchronized (queue) {
            queue.add(value);
        }
        synchronized (lock) {
            if (result != null)
                result = null;
            result = start();
        }
        return result;
    }

    private CompletableFuture<T> start() {
        var now  = Instant.now();
        var next = last.plus(cooldown);
        var time = Duration.between(now, next).toMillis();
        log.finer("wait for next = " + time);
        return supplyAsync(() -> null, delayedExecutor(time, TimeUnit.MILLISECONDS))
                .thenCompose(this::wrap);
    }

    private CompletableFuture<T> wrap(final @Nullable Object $) {
        return accumulate().thenCompose(it -> {
                    last = Instant.now();
                    synchronized (queue) {
                        if (!queue.isEmpty()) {
                            log.finer("there is still %d items in the queue, repeating".formatted(queue.size()));
                            return start();
                        }
                        return completedFuture(it);
                    }
                })
                .exceptionally(Polyfill.exceptionLogger());
    }

    private CompletableFuture<@Nullable T> accumulate() {
        return source.updateAndGet(stage -> stage
                .thenComposeAsync(src -> {
                    if (queue.isEmpty())
                        return completedFuture(src);
                    synchronized (queue) {
                        return accumulator.apply(src, queue);
                    }
                }));
    }
}
