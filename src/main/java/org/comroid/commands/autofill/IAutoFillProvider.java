package org.comroid.commands.autofill;

import org.comroid.commands.impl.CommandUsage;

import java.util.function.Predicate;
import java.util.stream.Stream;

@FunctionalInterface
public interface IAutoFillProvider {
    Stream<String> autoFill(CommandUsage usage, String argName, String currentValue);

    default Predicate<String> stringCheck(String currentValue) {
        return str -> {
            if (currentValue.isBlank() || currentValue.endsWith(" ")) return true;
            return currentValue.contains("*")
                   // wildcard mode
                   ? str.toLowerCase().matches(currentValue.toLowerCase().replace("*", "(\\*|.*?)"))
                   // normal filter
                   : str.toLowerCase().startsWith(currentValue.toLowerCase());
        };
    }
}
