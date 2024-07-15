package org.comroid.api.func;

import org.comroid.api.func.ext.Wrap;

import java.util.function.Function;

public interface ValueProvider<I, O> extends Function<I, O> {
    default Wrap<O> wrap(final I param) {
        return () -> get(param);
    }

    O get(I param);

    @Override
    default O apply(I i) {
        return get(i);
    }
}
