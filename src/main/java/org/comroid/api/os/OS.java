package org.comroid.api.os;

import org.comroid.api.DelegateStream;
import org.comroid.api.Event;
import org.comroid.api.Named;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

public enum OS implements Named {
    WINDOWS(".dll", "win") {
        @Override
        public CompletableFuture<Long> getRamUsage(long pid) {
            return CompletableFuture.completedFuture(0L); // todo
        }
    },
    MAC(".so", "mac"),
    UNIX(".so", "nix", "nux", "aix"),
    SOLARIS(".so", "sunos") {
        @Override
        public CompletableFuture<Long> getRamUsage(long pid) {
            return CompletableFuture.completedFuture(0L); // todo
        }
    };

    public static final OS current;
    public static final boolean isWindows;
    public static final boolean isMac;
    public static final boolean isUnix;
    public static final boolean isSolaris;

    static {
        current = detect();
        isWindows = current == WINDOWS;
        isMac = current == MAC;
        isUnix = current == UNIX;
        isSolaris = current == SOLARIS;
    }

    private final String libExtension;
    private final List<String> validators;

    public String getLibraryExtension() {
        return libExtension;
    }

    OS(String libExtension, String... validators) {
        this.libExtension = libExtension;
        this.validators = Collections.unmodifiableList(Arrays.asList(validators));
    }

    private static OS detect() {
        if (current != null)
            return current;

        String osName = System.getProperty("os.name");
        return findByName(osName);
    }

    public @NotNull
    static OS findByName(String osName) {
        for (OS value : values()) {
            final String osLow = osName.toLowerCase();
            if (value.validators.stream().anyMatch(osLow::contains))
                return value;
        }

        throw new NoSuchElementException("Unknown OS: " + osName);
    }

    public CompletableFuture<Long> getRamUsage(long pid) {
        var bus = new Event.Bus<String>();
        var future = bus.listen()
                .setKey(DelegateStream.IO.EventKey_Output)
                .once()
                .thenApply(Event::getData)
                .thenApply(str -> str == null || str.isBlank() ? "0" : str)
                .thenApply(str -> str.replaceAll("[\n ]", ""))
                .thenApply(Long::parseLong);
        var exec = DelegateStream.IO.execute("ps", "-o", "rss=", "-p", String.valueOf(pid))
                .redirectToEventBus(bus)
                .addChildren(bus);
        future.thenRun(exec::close);
        return future;
    }
}
