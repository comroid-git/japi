package org.comroid.commands.autofill.impl;

import org.comroid.commands.autofill.model.StringBasedAutoFillProvider;
import org.comroid.commands.impl.CommandUsage;

import java.util.stream.Stream;

public interface ObjectAutoFillAdapter<T> extends StringBasedAutoFillProvider {
    @Override
    default Stream<String> strings(CommandUsage usage, String currentValue) {
        return objects(usage, currentValue).map(this::toString);
    }

    String toString(T object);

    Stream<T> objects(CommandUsage usage, String currentValue);
}
