package org.comroid.api.attr;

import org.comroid.api.info.Log;

import java.util.logging.Logger;

@Deprecated
public interface LoggerCarrier {
    default Logger getLogger() {
        if (this instanceof Named)
            return Log.get(((Named) this).getName());
        return Log.get(getClass());
    }
}
