package org.comroid.api.html.form;

import org.jetbrains.annotations.Nullable;

public interface HtmlReadonlyStringInputDesc extends HtmlStringInputDesc {
    @Override
    @Nullable
    default String[] getHtmlExtraAttributes() {
        return new String[]{ "readonly" };
    }
}
