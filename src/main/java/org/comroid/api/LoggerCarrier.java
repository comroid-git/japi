package org.comroid.api;

import org.comroid.api.info.Log;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

import java.util.logging.Logger;

@Deprecated
public interface LoggerCarrier {
    default Logger getLogger() {
        if (this instanceof Named)
            return Log.get(((Named) this).getName());
        return Log.get(getClass());
    }
}
