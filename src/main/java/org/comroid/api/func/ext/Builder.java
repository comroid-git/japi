package org.comroid.api.func.ext;

import org.comroid.api.func.Provider;

public interface Builder<T> extends Provider.Now<T> {
    @Override
    default T now() {
        return build();
    }

    T build();
}
