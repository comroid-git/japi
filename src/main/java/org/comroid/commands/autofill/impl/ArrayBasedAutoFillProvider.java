package org.comroid.commands.autofill.impl;

import lombok.Value;
import org.comroid.commands.autofill.model.StringBasedAutoFillProvider;
import org.comroid.commands.impl.CommandUsage;

import java.util.stream.Stream;

import static java.util.stream.Stream.*;

@Value
public class ArrayBasedAutoFillProvider implements StringBasedAutoFillProvider {
    String[] options;

    public ArrayBasedAutoFillProvider(String... options) {
        this.options = options;
    }

    @Override
    public Stream<String> strings(CommandUsage usage, String currentValue) {
        return of(options);
    }
}
