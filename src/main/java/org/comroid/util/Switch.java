package org.comroid.util;

import lombok.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class Switch<I, O> implements Function<I, @Nullable O> {
    @Singular
    List<Case> cases = new ArrayList<>();
    @Nullable Supplier<O> defaultValue;

    @Contract(pure = true, value = "_,_ -> this")
    public Switch<I, O> option(I value, final O result) {
        return option(value, () -> result);
    }

    @Contract(pure = true, value = "_,_ -> this")
    public Switch<I, O> option(I value, Supplier<O> supplier) {
        return option(value::equals, supplier);
    }

    @Contract(pure = true, value = "_,_ -> this")
    public Switch<I, O> option(Predicate<I> predicate, final O result) {
        return option(predicate, () -> result);
    }

    @Contract(pure = true, value = "_,_ -> this")
    public Switch<I, O> option(Predicate<I> predicate, Supplier<O> supplier) {
        return option(new Case(predicate, supplier));
    }

    @Contract(pure = true, value = "_ -> this")
    public Switch<I, O> option(Case it) {
        cases.add(it);
        return this;
    }

    @Override
    public @Nullable O apply(final @NotNull I input) {
        return cases.stream()
                .filter(c -> c.predicate.test(input))
                .findAny()
                .map(c -> c.supplier.get())
                .orElseGet(defaultValue != null ? defaultValue : () -> null);
    }

    @Value
    public class Case {
        Predicate<I> predicate;
        Supplier<O> supplier;
    }
}
