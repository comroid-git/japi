package org.comroid.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.util.StackTraceUtils;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

public interface LoggerCarrier {
    @OverrideOnly
    default Logger getLogger() {
        return LogManager.getLogger(StackTraceUtils.callerClass(1));
    }
}
