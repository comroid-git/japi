package org.comroid.commands.autofill;

import org.comroid.api.attr.Named;
import org.jetbrains.annotations.NotNull;

public record AutoFillOption(String key, String description) implements Named, CharSequence {
    @Override
    public String getName() {
        return key;
    }

    @Override
    public String getAlternateName() {
        return key + " (" + description + ")";
    }

    @Override
    public int length() {
        return key.length();
    }

    @Override
    public char charAt(int index) {
        return key.charAt(index);
    }

    @Override
    public @NotNull CharSequence subSequence(int start, int end) {
        return key.subSequence(start, end);
    }

    @Override
    public @NotNull String toString() {
        return getPrimaryName();
    }
}
