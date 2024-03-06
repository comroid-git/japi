package org.comroid.api.html.form;

public interface HtmlStringInputDesc extends HtmlInputDesc {
    @Override
    default String getHtmlInputType() {
        return "text";
    }
}
