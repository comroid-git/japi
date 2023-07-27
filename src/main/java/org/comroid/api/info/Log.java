package org.comroid.api.info;

import lombok.experimental.UtilityClass;
import org.comroid.util.StackTraceUtils;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public final class Log extends Logger {
    private Log(String name) {
        super(name, null);
    }

    public static Logger get() {
        return getForCaller(1);
    }

    public static Logger get(String name) {
        var logger = new Log(name);
        LogManager.getLogManager().addLogger(logger);
        return logger;
    }

    public static Logger get(Class<?> cls) {
        return get(cls.getCanonicalName());
    }

    public static void at(Level level, String message) {
        getForCaller(1).log(level, message);
    }

    private static Logger getForCaller(int skip) {
        return get(StackTraceUtils.callerClass(skip + 1));
    }
}
