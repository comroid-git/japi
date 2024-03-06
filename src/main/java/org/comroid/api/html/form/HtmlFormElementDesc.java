package org.comroid.api.html.form;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;

public interface HtmlFormElementDesc {
    @Language(value = "HTML", prefix = "<", suffix = ">")
    String getHtmlTagName();

    @Language(value = "HTML", prefix = "<input ", suffix = ">")
    @Nullable
    default String[] getHtmlExtraAttributes() {
        return new String[0];
    }
}
