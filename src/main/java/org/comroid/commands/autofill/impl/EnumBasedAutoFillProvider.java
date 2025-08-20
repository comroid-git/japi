package org.comroid.commands.autofill.impl;

import lombok.Value;
import org.comroid.commands.impl.CommandUsage;

import java.util.Arrays;
import java.util.stream.Stream;

@Value
public class EnumBasedAutoFillProvider<T extends java.lang.Enum<? super T>> implements ObjectAutoFillAdapter<T> {
    Class<T> type;

    @Override
    public String toString(T object) {
        return object.name();
    }

    @Override
    public Stream<T> objects(CommandUsage usage, String currentValue) {
        return Arrays.stream(type.getEnumConstants());
    }
}
