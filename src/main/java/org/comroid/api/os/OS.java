package org.comroid.api.os;

import lombok.SneakyThrows;
import org.comroid.api.func.util.DelegateStream;
import org.comroid.api.func.util.Event;
import org.comroid.api.attr.Named;
import org.comroid.api.info.Log;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum OS implements Named {
    WINDOWS(".dll", "win") {
        @Override
        public CompletableFuture<Long> getRamUsage(long pid) {
            return CompletableFuture.completedFuture(0L); // todo
        }

        @Override
        protected String getHostsFilePath() {
            return "C:\\Windows\\System32\\drivers\\etc\\hosts";
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

    public static final Pattern HostsPattern = Pattern.compile("^(?<ip>(?<ipv4>\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|(?<ipv6>[0-9a-fA-F]{4}?::\\d))\\s+?(?<hostname>[\\w-_.]+)(\\s+?(?<local>\\w+))?$");
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

    @SneakyThrows
    public Host getPrimaryHost() {
        try (var in = new FileInputStream(getHostsFilePath());
             var isr = new InputStreamReader(in);
             var buf = new BufferedReader(isr)) {
            return buf.lines()
                    .map(String::trim)
                    .filter(s->!s.startsWith("#")&& !s.isBlank())
                    .map(s->s.contains("#")?s.substring(0,s.indexOf('#')):s)
                    .map(String::trim)
                    .map(HostsPattern::matcher)
                    .filter(Matcher::matches)
                    .map(matcher->new Host(matcher.group("ip"),matcher.group("hostname")))
                    .findFirst()
                    .orElse(Host.Local);
        } catch (Throwable t) {
            Log.at(Level.FINE, "Could not get primary Hostname", t);
            return Host.Local;
        }
    }

    protected String getHostsFilePath() {
        return "/etc/hosts";
    }

    public record Host(String ip, String name){
        public static final Host Local = new Host("127.0.0.1", "localhost");
    }
}
