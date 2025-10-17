package org.comroid.api.html.form;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;

public interface HtmlFormElementDesc {
    @Deprecated
    @Language(value = "HTML", prefix = "<", suffix = ">")
    String getHtmlTagName();

    @Language(value = "HTML", prefix = "<input type = \"", suffix = "\">")
    default String getHtmlInputType() {
        return getHtmlTagName(); // todo: migrate broken wording
    }

    @Language(value = "HTML", prefix = "<input ", suffix = ">")
    @Nullable
    default String[] getHtmlExtraAttributes() {
        return new String[0];
    }
}
