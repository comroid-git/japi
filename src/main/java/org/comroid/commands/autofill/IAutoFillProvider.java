package org.comroid.commands.autofill;

import org.comroid.commands.impl.CommandUsage;

import java.util.function.Predicate;
import java.util.stream.Stream;

@FunctionalInterface
public interface IAutoFillProvider {
    static Predicate<CharSequence> stringCheck(String currentValue) {
        return str -> {
            if (currentValue.isBlank() || currentValue.endsWith(" ")) return true;
            return currentValue.contains("*")
                   // wildcard mode
                   ? str.toString().toLowerCase().matches(currentValue.toLowerCase().replace("*", "(\\*|.*?)"))
                   // normal filter
                   : str.toString().toLowerCase().startsWith(currentValue.toLowerCase());
        };
    }

    Stream<? extends CharSequence> autoFill(CommandUsage usage, String argName, String currentValue);
}
