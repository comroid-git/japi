package org.comroid.api;

import java.util.function.Function;
import java.util.function.ToIntFunction;

public interface ThrowingToIntFunction<I, T extends Throwable> {
    default ToIntFunction<I> wrap() {return wrap(RuntimeException::new);}
    default ToIntFunction<I> wrap(Function<Throwable, ? extends RuntimeException> remapper) {
        return in -> {
            try {
                return applyAsInt(in);
            } catch (Throwable error) {
                if (remapper != null)
                    throw remapper.apply(error);
                return -1;
            }
        };
    }

    int applyAsInt(I in) throws T;
}
