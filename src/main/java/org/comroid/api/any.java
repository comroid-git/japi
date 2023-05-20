package org.comroid.api;

import org.comroid.api.info.Described;
import org.jetbrains.annotations.Nullable;

public interface any extends
        // informational
        Named, Described,
        // object extensions
        Upgradeable, SelfRef<any>, ValueBox<any>,
        // utility
        Context,
        // for the sake of having it
        LoggerCarrier, LifeCycle, UncheckedCloseable {
    any NULL = new any(){};

    @Override
    default any getValue() {
        return this;
    }
}
