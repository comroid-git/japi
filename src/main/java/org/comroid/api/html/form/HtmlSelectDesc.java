package org.comroid.api.html.form;

import java.util.Map;

public interface HtmlSelectDesc extends HtmlFormElementDesc {
    Map<String, String> getHtmlSelectOptions();

    @Override
    default String getHtmlTagName() {
        return "select";
    }
}
