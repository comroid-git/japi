package org.comroid.util;

import lombok.Getter;
import org.comroid.api.TextDecoration;

public enum Markdown implements TextDecoration {
    @Italic Italic("_"),
    @Bold Bold("**"),
    @Underline Underline("__"),
    @Strikethrough Strikethrough("~~"),

    @Verbatim Code("`"),
    @Verbatim CodeBlock("```"),

    None("");

    @Getter private final String wrap;

    Markdown(String wrap) {
        this.wrap = wrap;
    }

    @Override
    public CharSequence getPrefix() {
        return wrap;
    }

    @Override
    public CharSequence getSuffix() {
        return wrap;
    }

    @Override
    public String getPrimaryName() {
        return wrap;
    }

    @Override
    public String getAlternateName() {
        return name();
    }
}
