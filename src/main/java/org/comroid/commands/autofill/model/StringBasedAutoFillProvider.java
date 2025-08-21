package org.comroid.commands.autofill.model;

import org.comroid.commands.autofill.IAutoFillProvider;
import org.comroid.commands.impl.CommandUsage;

import java.util.stream.Stream;

public interface StringBasedAutoFillProvider extends IAutoFillProvider {
    @Override
    default Stream<String> autoFill(CommandUsage usage, String argName, String currentValue) {
        return strings(usage, currentValue).filter(IAutoFillProvider.stringCheck(currentValue));
    }

    Stream<String> strings(CommandUsage usage, String currentValue);
}
