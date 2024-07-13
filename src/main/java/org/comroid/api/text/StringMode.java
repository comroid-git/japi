package org.comroid.api.text;

import org.comroid.annotations.Default;
import org.comroid.api.attr.Named;

public enum StringMode implements Named {
    @Default NORMAL,
    GREEDY,
    SINGLE_WORD
}
