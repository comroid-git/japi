package org.comroid.util;

import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;
import org.comroid.api.Polyfill;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static java.util.concurrent.CompletableFuture.*;

@Log
@Value
public class Ratelimit<T, Acc> {
    static Map<Object, Ratelimit<?, ?>> cache = new ConcurrentHashMap<>();

    public static <T, Acc> CompletableFuture<T> run(
            Acc value,
            Duration cooldown,
            AtomicReference<CompletableFuture<@Nullable T>> source,
            BiFunction<@NotNull T, Queue<@NotNull Acc>, CompletableFuture<@Nullable T>> task
    ) {
        return Polyfill.<Ratelimit<T, Acc>>uncheckedCast(cache.computeIfAbsent(task.getClass(),
                $ -> new Ratelimit<>(cooldown, source, task))).push(value);
    }

    @NotNull Duration cooldown;
    @NotNull AtomicReference<CompletableFuture<@Nullable T>> source;
    @NotNull BiFunction<T, Queue<Acc>, CompletableFuture<T>> accumulator;
    @NotNull Queue<Acc> queue = new LinkedBlockingQueue<>();
    @NonFinal
    @Nullable CompletableFuture<T> result;
    @NonFinal
    Instant last = Instant.EPOCH;

    public Ratelimit(@NotNull Duration cooldown,
                     @NotNull AtomicReference<CompletableFuture<@Nullable T>> source,
                     @NotNull BiFunction<T, Queue<Acc>, CompletableFuture<T>> accumulator) {
        this.cooldown = cooldown;
        this.source = source;
        this.accumulator = accumulator;
    }

    @SneakyThrows
    public synchronized CompletableFuture<T> push(Acc value) {
        synchronized (queue) {
            queue.add(value);
        }
        if (result != null && result.isDone())
            result = null;
        if (result == null)
            result = start();
        return result;
    }

    private CompletableFuture<T> start() {
        var now = Instant.now();
        var next = last.plus(cooldown);
        var time = Duration.between(now, next).toMillis();
        log.fine("wait for next = " + time);
        return supplyAsync(() -> null, delayedExecutor(time, TimeUnit.MILLISECONDS))
                .thenCompose(this::wrap);
    }

    private CompletableFuture<T> wrap(final @Nullable Object $) {
        return accumulate().thenCompose(it -> {
                    last = Instant.now();
                    synchronized (queue) {
                        if (!queue.isEmpty()) {
                            log.fine("there is still %d items in the queue, repeating".formatted(queue.size()));
                            return start();
                        }
                        return completedFuture(it);
                    }
                })
                .exceptionally(Polyfill.exceptionLogger());
    }

    private CompletableFuture<@Nullable T> accumulate() {
        return source.updateAndGet(stage -> stage
                .thenCompose(src -> {
                    if (queue.isEmpty())
                        return completedFuture(src);
                    synchronized (queue) {
                        return accumulator.apply(src, queue);
                    }
                }));
    }
}
