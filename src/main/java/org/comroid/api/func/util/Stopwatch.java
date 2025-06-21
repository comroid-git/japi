package org.comroid.api.func.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.System.*;

@Getter
@RequiredArgsConstructor
public final class Stopwatch {
    public static final Map<Object, Stopwatch> cache = new ConcurrentHashMap<>();

    public static Stopwatch start(Object key) {
        return get(key).start();
    }

    public static Stopwatch get(Object key) {
        return cache.computeIfAbsent(key, Stopwatch::new);
    }

    public static Duration stop(Object key) {
        return get(key).stop();
    }

    private final Object key;
    private       long   start;

    public Stopwatch start() {
        start = nanoTime();
        return this;
    }

    public Duration stop() {
        return Duration.ofNanos(nanoTime() - start);
    }
}
