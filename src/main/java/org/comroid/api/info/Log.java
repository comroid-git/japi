package org.comroid.api.info;

import lombok.SneakyThrows;
import org.comroid.api.java.StackTraceUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class Log {
    private Log() {
        throw new UnsupportedOperationException();
    }

    public static Logger get() {
        return getForCaller(1);
    }

    public static Logger get(String name) {
        return Logger.getLogger(Objects.requireNonNullElse(name, ""));
    }

    public static Logger get(Class<?> cls) {
        return get(cls.getCanonicalName());
    }

    @SneakyThrows
    public static void sOutMP(Level level, Object... parameter) {
        final var e = new Throwable().getStackTrace()[1];
        get(Class.forName(e.getClassName())).log(level, "%s.%s(%s)".formatted(e.getClassName(), e.getMethodName(),
                Arrays.stream(parameter)
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "))));
    }

    public static void at(Level level, String message, Throwable t) {
        getForCaller(1).log(level, message, t);
    }

    public static void at(Level level, String message) {
        getForCaller(1).log(level, message);
    }

    private static Logger getForCaller(int skip) {
        return get(StackTraceUtils.callerClass(skip + 1));
    }
}
