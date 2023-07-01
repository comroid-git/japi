package org.comroid.api.info;

import lombok.experimental.UtilityClass;
import org.comroid.util.StackTraceUtils;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

@UtilityClass
public class Log {
    public Logger get() {
        return getForCaller(1);
    }

    public Logger get(String name) {
        return LogManager.getLogManager().getLogger(name);
    }

    public Logger get(Class<?> cls) {
        return get(cls.getCanonicalName());
    }

    public void at(Level level, String message) {
        getForCaller(1).log(level, message);
    }

    private Logger getForCaller(int skip) {
        return get(StackTraceUtils.callerClass(skip + 1));
    }
}
