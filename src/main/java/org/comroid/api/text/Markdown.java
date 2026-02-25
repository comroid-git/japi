package org.comroid.api.text;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public enum Markdown implements TextDecoration {
    @Italic Italic("_"),
    @Bold Bold("**"),
    @Underline Underline("__"),
    @Strikethrough Strikethrough("~~"),

    @Verbatim Code("`"),
    @Verbatim CodeBlock("```\n", "\n```"),

    @Quote Quote("> ", "\n"),

    None("");

    private final @Nullable String wrap;
    private final @Nullable String prefix;
    private final @Nullable String suffix;

    Markdown(@NotNull String prefix, @NotNull String suffix) {
        this.wrap = null;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    Markdown(@NotNull String wrap) {
        this.wrap = wrap;
        this.prefix = null;
        this.suffix = null;
    }

    @Override
    public String getPrimaryName() {
        return getPrefix().toString();
    }

    @Override
    public String getAlternateName() {
        return name();
    }

    @NotNull
    @Override
    public CharSequence getPrefix() {
        if (wrap == null) {
            assert prefix != null;
            return prefix;
        } else return wrap;
    }

    @Override
    public CharSequence getSuffix() {
        return wrap == null ? suffix : wrap;
    }
}
