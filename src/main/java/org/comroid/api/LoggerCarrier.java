package org.comroid.api;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public interface LoggerCarrier {
    @OverrideOnly
    default Logger getLogger() {
        if (this instanceof Named)
            return LoggerFactory.getLogger(((Named) this).getName());
        return LoggerFactory.getLogger(getClass());
    }
}
