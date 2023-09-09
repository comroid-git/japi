package org.comroid.api.info;

import lombok.experimental.UtilityClass;
import org.comroid.util.StackTraceUtils;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

@UtilityClass
public final class Log {
    public static Logger get() {
        return getForCaller(1);
    }

    public static Logger get(String name) {
        return Logger.getLogger(name);
    }

    public static Logger get(Class<?> cls) {
        return get(cls.getCanonicalName());
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
