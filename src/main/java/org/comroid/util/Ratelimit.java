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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static java.util.concurrent.CompletableFuture.*;

@Log
@Value
public class Ratelimit<T, Acc> {
    static Map<Object, Ratelimit<?, ?>> cache = new ConcurrentHashMap<>();
    @NotNull Duration cooldown;
    @NotNull AtomicReference<CompletableFuture<@Nullable T>> source;
    @NotNull BiFunction<T, Queue<Acc>, CompletableFuture<T>> accumulator;
    @NotNull Queue<Acc> queue = new LinkedBlockingQueue<>();
    @NonFinal @Nullable CompletableFuture<T> result;
    @NonFinal Instant last = Instant.EPOCH;

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
        System.out.println("Ratelimit.push('" + value+"')");
        if (result != null && result.isDone())
            result = null;
        if (result == null) {
            var now = Instant.now();
            var next = last.plus(cooldown);
            var time = Duration.between(now, next).toMillis();
            log.info("wait for next = " + time);
            result = (time <= 0 ? completedFuture(null)
                    : supplyAsync(() -> null, delayedExecutor(time, TimeUnit.MILLISECONDS)))
                    .thenCompose($ -> source.updateAndGet(stage -> stage
                                    .thenCompose(src -> {
                                        if (queue.isEmpty())
                                            return completedFuture(src);
                                        synchronized (queue) {
                                            return accumulator.apply(src, queue);
                                        }
                                    }))
                            .thenApply(it -> {
                                if (it != null) {
                                    last = Instant.now();
                                }
                                return it;
                            })
                            .exceptionally(Polyfill.exceptionLogger()));
        }
        return result;
    }

    public static <T, Acc> CompletableFuture<T> run(
            Acc value,
            Duration cooldown,
            AtomicReference<CompletableFuture<@Nullable T>> source,
            BiFunction<@NotNull T, Queue<@NotNull Acc>, CompletableFuture<@Nullable T>> task
    ) {
        return Polyfill.<Ratelimit<T, Acc>>uncheckedCast(cache.computeIfAbsent(task.getClass(),
                $-> new Ratelimit<>(cooldown, source, task))).push(value);
    }
}
