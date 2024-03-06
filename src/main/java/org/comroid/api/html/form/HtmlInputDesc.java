package org.comroid.api.html.form;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;

public interface HtmlInputDesc extends HtmlFormElementDesc {
    @Language(value = "HTML", prefix = "<input type=\"", suffix = "\">")
    String getHtmlInputType();

    @Override
    default String getHtmlTagName() {
        return "input";
    }
}
