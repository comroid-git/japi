package org.comroid.api.func.util;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@Data
public class StateEngine<T, S> implements Predicate<T> {
    private final AtomicReference<S> state;
    private final BiFunction<T, S, S> update;
    private final Predicate<S> check;

    public StateEngine(S init, BiFunction<T, S, S> update, Predicate<S> check) {
        this.state = new AtomicReference<>(init);
        this.update = update;
        this.check = check;
    }

    @Override
    public boolean test(T item) {
        return check.test(state.updateAndGet(state -> update.apply(item, state)));
    }
}
