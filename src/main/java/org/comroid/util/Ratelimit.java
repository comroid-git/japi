package org.comroid.util;

import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Value
public class Ratelimit implements Executor {
    static Map<Object, Ratelimit> cache = new ConcurrentHashMap<>();
    Deque<Runnable> tasks;
    Duration cooldown;
    @NonFinal Instant last;
    @NonFinal @Nullable CompletableFuture<?> timeout;

    private Ratelimit(Runnable task, Duration cooldown) {
        this.tasks = new ArrayDeque<>() {{
            add(task);
        }};
        this.cooldown = cooldown;
        this.last = Instant.EPOCH;
    }

    @Override
    @SneakyThrows
    public synchronized void execute(@NotNull Runnable task) {
        var now = Instant.now();
        var next = last.plus(cooldown);
        if (next.isBefore(now)) {
            task.run();
            last = now;
        } else {
            if (timeout == null) timeout = new CompletableFuture<>()
                    .completeOnTimeout(null, next.toEpochMilli()-now.toEpochMilli(), TimeUnit.MILLISECONDS)
                    .thenRun(()->{
                        while(!tasks.isEmpty())
                            tasks.poll().run();
                        timeout = null;
                        last = Instant.now();
                    });
            tasks.add(task);
        }
    }

    public static void run(@NotNull Duration cooldown, @NotNull Runnable task){
        cache.computeIfAbsent(task.getClass(), $->new Ratelimit(task,cooldown)).execute(task);
    }
}
