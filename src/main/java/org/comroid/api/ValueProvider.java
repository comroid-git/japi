package org.comroid.api;

import java.util.function.Function;

public interface ValueProvider<I, O> extends Function<I, O> {
    O get(I param);

    default SupplierX<O> wrap(final I param) {
        return () -> get(param);
    }

    @Override
    default O apply(I i) {
        return get(i);
    }
}
