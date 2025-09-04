package org.comroid.commands.autofill.impl;

import org.comroid.annotations.Instance;
import org.comroid.commands.autofill.model.StringBasedAutoFillProvider;
import org.comroid.commands.impl.CommandUsage;

import java.util.Arrays;
import java.util.stream.Stream;

import static java.util.function.Predicate.*;
import static java.util.stream.Stream.*;

public enum DurationAutoFillProvider implements StringBasedAutoFillProvider {
    @Instance INSTANCE;

    private static final String[] suffixes = new String[]{ "min", "h", "d", "w", "Mon", "y" };
    private static final int[]    charMask = Arrays.stream(suffixes).flatMapToInt(String::chars).distinct().toArray();
    private static final int[]    longMask = Arrays.stream(suffixes)
            .flatMapToInt(str -> str.chars().skip(1))
            .distinct()
            .toArray();

    @Override
    public Stream<String> strings(CommandUsage usage, String currentValue) {
        if (currentValue.isEmpty())
            // example values
            return of("5m", "6h", "3d", "2w", "6Mon", "1y");

        // find last char
        var chars = currentValue.toCharArray();
        var lc    = chars[chars.length - 1];

        var offset = 0;
        if (Arrays.binarySearch(charMask, lc) != -1) offset += 1;
        if (Arrays.binarySearch(longMask, lc) != -1) offset += 1;
        if (lc == 'n') offset += 1;

        final var cutoff = offset;
        return Arrays.stream(suffixes)
                .filter(str -> str.length() - 1 >= cutoff)
                .map(str -> str.substring(cutoff))
                .filter(not(String::isBlank))
                .map(str -> currentValue + str);
    }
}
