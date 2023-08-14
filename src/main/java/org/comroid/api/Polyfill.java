package org.comroid.api;

import lombok.experimental.UtilityClass;
import org.comroid.util.RegExpUtil;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.net.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.isNull;

@Experimental
@UtilityClass
public final class Polyfill {
    @Deprecated
    public static final String UUID_PATTERN = RegExpUtil.UUID4.pattern();
    private static final CompletableFuture<?> infiniteFuture = new CompletableFuture<>();

    public static <T> T supplyOnce(Provider<T> provider, Function<T, T> writer, Supplier<T> accessor) {
        final T accessed = accessor.get();
        return accessed == null ? (writer.apply(provider.now())) : accessed;
    }

    public static String regexGroupOrDefault(
            Matcher matcher, String groupName, @Nullable String orDefault
    ) {
        String cont;

        if (matcher.matches() && (cont = matcher.group(groupName)) != null) {
            return cont;
        } else if (orDefault != null) {
            return orDefault;
        } else {
            throw new NullPointerException("Group cannot be matched!");
        }
    }

    public static <R, T extends Throwable> Function<T, R> exceptionLogger() {
        return nil -> {
            nil.printStackTrace(System.err);

            return null;
        };
    }

    public static <R, T extends Throwable> Function<T, R> exceptionLogger(final Logger logger) {
        return exceptionLogger(logger, "An async error occurred");
    }

    public static <R, T extends Throwable> Function<T, R> exceptionLogger(final Logger logger, final String message) {
        return exceptionLogger(logger, Level.SEVERE, message);
    }

    public static <R, T extends Throwable> Function<T, R> exceptionLogger(final Logger logger, final Level level, final String message) {
        return throwable -> {
            logger.log(level, message, throwable);
            return null;
        };
    }

    public static <T extends Throwable> URL url(
            String spec
    ) throws T {
        return url(spec, null);
    }

    public static <T extends Throwable> URL url(
            String spec, @Nullable Function<MalformedURLException, T> throwableReconfigurator
    ) throws T {
        if (throwableReconfigurator == null) {
            //noinspection unchecked
            throwableReconfigurator = cause -> (T) new AssertionError(cause);
        }

        try {
            return new URL(spec);
        } catch (MalformedURLException e) {
            throw throwableReconfigurator.apply(e);
        }
    }

    public static URL url(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends Throwable> URI uri(
            String spec
    ) throws T {
        return uri(spec, null);
    }

    public static <T extends Throwable> URI uri(
            String spec, Function<URISyntaxException, T> throwableReconfigurator
    ) throws T {
        if (throwableReconfigurator == null) {
            throwableReconfigurator = cause -> (T) new AssertionError(cause);
        }

        try {
            return new URI(spec);
        } catch (URISyntaxException e) {
            throw throwableReconfigurator.apply(e);
        }
    }

    public static <T> T notnullOr(@Nullable T value, @NotNull T def) {
        if (isNull(value)) {
            return Objects.requireNonNull(def, "Default value cannot be null");
        }
        return value;
    }

    @Contract("_ -> param1")
    public static <R> R uncheckedCast(Object instance) {
        //noinspection unchecked
        return (R) instance;
    }

    public static <T, R> Function<T, R> failingFunction(Supplier<? extends RuntimeException> exceptionSupplier) {
        return new Function<T, R>() {
            private final Supplier<? extends RuntimeException> supplier = exceptionSupplier;

            @Override
            public R apply(T t) {
                throw supplier.get();
            }
        };
    }

    public static <T, R> Function<T, R> erroringFunction(@Nullable String message) {
        return new Function<T, R>() {
            private final String msg = notnullOr(message, "Unexpected Call");

            @Override
            public R apply(T t) {
                throw new AssertionError(msg);
            }
        };
    }

    public static Object selfawareObject() {
        //noinspection FieldCanBeLocal
        class Monitor {
            private final WeakReference<Monitor> self;

            {
                this.self = new WeakReference<>(this);
            }

            @Override
            public String toString() {
                return String.format("SelfAwareLock@%s", Integer.toHexString(hashCode()));
            }
        }

        return new Monitor();
    }

    public static <T> CompletableFuture<T> failedFuture(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    public static <T> Supplier<T> constantSupplier(T it) {
        return new Supplier<T>() {
            private final T value = it;

            @Override
            public T get() {
                return value;
            }
        };
    }

    public static <T> CompletableFuture<T> infiniteFuture() {
        return uncheckedCast(infiniteFuture);
    }

    public static String[] splitStringForLength(String data, int maxLength) {
        String[] parts = new String[(data.length() / maxLength) + 1];

        for (int i = 0, end; i < parts.length; i++)
            parts[i] = data.substring(maxLength * i, (end = maxLength * (i + 1)) > data.length() ? data.length() : end);
        return parts;
    }

    public static Color parseHexColor(String hex) {
        if (!hex.startsWith("#"))
            throw new IllegalArgumentException("Invalid Hex-Color String: " + hex);
        return new Color(Integer.parseInt(hex.substring(1), 16));
    }

    public static String hexString(Color color) {
        return '#'
                + Integer.toHexString(color.getRed())
                + Integer.toHexString(color.getGreen())
                + Integer.toHexString(color.getBlue());
    }

    public static Inet4Address parseIPv4(String ipv4) {
        String[] split = ipv4.split("\\.");
        byte[] addr = new byte[4];
        if (split.length != addr.length)
            throw new IllegalArgumentException("Invalid IPv4 Address: " + ipv4);
        for (int i = 0; i < split.length; i++)
            addr[i] = (byte) Integer.parseInt(split[i]);
        try {
            return (Inet4Address) Inet4Address.getByAddress(addr);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IPv4 Address; host is unknown: " + ipv4);
        }
    }

    public static Inet6Address parseIPv6(String ipv6) {
        String[] split = ipv6.split(":");
        byte[] addr = new byte[8];
        if (split.length != addr.length)
            throw new IllegalArgumentException("Invalid IPv6 Address: " + ipv6);
        for (int i = 0; i < split.length; i++)
            addr[i] = (byte) Integer.parseInt(split[i], 16);
        try {
            return (Inet6Address) Inet6Address.getByAddress(addr);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IPv6 Address; host is unknown: " + ipv6);
        }
    }

    public static Map<String, String> getUriQuery(URI uri) {
        String query = uri.getQuery();
        if (query == null)
            return Collections.emptyMap();
        return Arrays.stream(query.split("&"))
                .map(entry -> entry.split("="))
                .filter(entry -> entry.length == 2)
                .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
    }

    public static String plural(Collection<?> list, String singular, String plural) {
        if (list.size()==1)
            return singular;
        if (plural.charAt(0) == '+')
            return singular + plural.substring(1);
        return plural;
    }

    @SafeVarargs
    public static <T> Stream<T> stream(Stream<? extends T>... streams) {
        if (streams.length == 0)
            return Stream.empty();
        var stream = streams[0];
        for (int i = 1; i < streams.length; i++)
            stream = Stream.concat(stream, streams[i]);
        return stream.map(Polyfill::uncheckedCast);
    }

    public static <T> Stream<Collection<T>> batches(final int maxSize, final Stream<T> stream) {
        final var split = stream.spliterator();
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.CONCURRENT) {
            @Override
            public boolean tryAdvance(Consumer<? super Collection<T>> action) {
                final var ls = new ArrayList<T>();
                var cont = true;
                while (ls.size() < maxSize && (cont = split.tryAdvance(ls::add)))
                    action.accept(ls);
                return cont;
            }
        }, true);
    }

    public static boolean updateBoolState(boolean current, boolean newState, Runnable rising, Runnable falling) {
        if (!current && newState)
            rising.run();
        else if (current && !newState)
            falling.run();
        return current != newState;
    }

    public static String durationString(Duration d) {
        long seconds = d.getSeconds();
        long absSeconds = Math.abs(seconds);
        var t = "";
        if (absSeconds > 60 * 60) {
            var diff = absSeconds / (60 * 60);
            t += diff + "h";
            absSeconds -= diff * 60 * 60;
        }
        if (absSeconds > 60) {
            var diff = absSeconds / 60;
            t += diff + "min";
            absSeconds -= diff * 60;
        }
        if (absSeconds > 0)
            t += (absSeconds) + "sec";
        return t;
    }
}
