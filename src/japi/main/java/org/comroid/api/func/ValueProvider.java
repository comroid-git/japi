package org.comroid.api.func;

import org.comroid.api.func.ext.Wrap;

import java.util.function.Function;

public interface ValueProvider<I, O> extends Function<I, O> {
    O get(I param);

    default Wrap<O> wrap(final I param) {
        return () -> get(param);
    }

    @Override
    default O apply(I i) {
        return get(i);
    }
}
