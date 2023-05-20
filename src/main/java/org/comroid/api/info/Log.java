package org.comroid.api.info;

import lombok.experimental.UtilityClass;
import org.comroid.util.StackTraceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

@UtilityClass
public class Log {
    public Logger get() {
        return getForCaller(1);
    }

    public Logger get(String name) {
        return LoggerFactory.getLogger(name);
    }

    public Logger get(Class<?> cls) {
        return LoggerFactory.getLogger(cls);
    }

    public void at(Level level, String message) {
        getForCaller(1).atLevel(level).log(message);
    }

    private Logger getForCaller(int skip) {
        return LoggerFactory.getLogger(StackTraceUtils.callerClass(skip + 1));
    }
}
