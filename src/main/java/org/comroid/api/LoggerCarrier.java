package org.comroid.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

public interface LoggerCarrier {
    @OverrideOnly
    default Logger getLogger() {
        if (this instanceof Named)
            return LogManager.getLogger(((Named) this).getName());
        return LogManager.getLogger(getClass());
    }
}
