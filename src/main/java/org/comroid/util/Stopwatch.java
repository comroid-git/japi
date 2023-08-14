package org.comroid.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.time.Instant.now;

@Getter
@RequiredArgsConstructor
public final class Stopwatch {
    public static final Map<Object, Stopwatch> cache = new ConcurrentHashMap<>();

    public static Stopwatch get(Object key) {
        return cache.computeIfAbsent(key, Stopwatch::new);
    }

    public static void start(Object key) {
        get(key).start();
    }

    public static Duration stop(Object key) {
        return get(key).stop();
    }

    private final Object key;
    private Instant start;

    public void start() {
        start = now();
    }

    public Duration stop() {
        return Duration.between(start, now());
    }
}
