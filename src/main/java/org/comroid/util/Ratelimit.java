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
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

@Log
@Value
public class Ratelimit<T, Acc> {
    static Map<Object, Ratelimit<?, ?>> cache = new ConcurrentHashMap<>();
    @NotNull Duration cooldown;
    @NotNull CompletableFuture<T> source;
    @NotNull BiFunction<T, Queue<Acc>, CompletableFuture<T>> accumulator;
    @NonFinal Instant last = Instant.EPOCH;
    @NonFinal @Nullable CompletableFuture<T> timeout;
    @NonFinal Queue<Acc> values = new ArrayDeque<>();

    public Ratelimit(@NotNull Duration cooldown,
                     @NotNull CompletableFuture<T> source,
                     @NotNull BiFunction<T, Queue<Acc>, CompletableFuture<T>> accumulator) {
        this.cooldown = cooldown;
        this.source = source;
        this.accumulator = accumulator;
    }

    @SneakyThrows
    public synchronized CompletableFuture<T> push(Acc value) {
        if (timeout != null && timeout.isDone())
            timeout = null;
        values.add(value);
        var now = Instant.now();
        var next = last.plus(cooldown);
        if (timeout == null) {
            var time = Duration.between(now, next).toMillis();
            log.info("timeout:"+time);
            timeout = (time <= 0 ? CompletableFuture.completedFuture(values)
                    : new CompletableFuture<Queue<Acc>>().completeOnTimeout(values, time, TimeUnit.MILLISECONDS))
                    .thenCombine(source, (queue,src) -> {
                        if (queue.isEmpty())
                            return CompletableFuture.<T>completedFuture(null);
                        return accumulator.apply(src,queue);
                    })
                    .thenCompose(Function.identity())
                    .thenApply(it->{
                        if (it != null)
                            last = Instant.now();
                        values = new ArrayDeque<>();
                        return it;
                    });
        }
        return timeout;
    }

    public static <T, Acc> CompletableFuture<T> run(
            Acc value,
            Duration cooldown,
            CompletableFuture<@Nullable T> source,
            BiFunction<T, Queue<Acc>, CompletableFuture<T>> task
    ) {
        return Polyfill.<Ratelimit<T, Acc>>uncheckedCast(cache.computeIfAbsent(task.getClass(),
                $-> new Ratelimit<>(cooldown, source, task))).push(value);
    }
}
