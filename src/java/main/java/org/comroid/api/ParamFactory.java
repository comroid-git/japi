package org.comroid.api;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface ParamFactory<P, T> extends Provider.Now<T> {
    @Override
    default T now() {
        return create();
    }

    default T create() {
        return create(null);
    }

    T create(@Nullable P parameter);

    int counter();

    int peekCounter();

    class Abstract<P, T> implements ParamFactory<P, T> {
        private final Function<P, T> factory;
        protected int counter = 0;

        public Abstract(Function<P, T> factory) {
            this.factory = factory;
        }

        protected Abstract() {
            this.factory = null;
        }

        @Override
        public T create(@Nullable P parameter) {
            if (factory == null) {
                throw new AbstractMethodError();
            }

            return factory.apply(parameter);
        }

        @Override
        public final int counter() {
            return counter++;
        }

        @Override
        public final int peekCounter() {
            return counter;
        }
    }
}