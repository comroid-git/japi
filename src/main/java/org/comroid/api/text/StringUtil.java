package org.comroid.api.text;

public final class StringUtil {
    public static String maxLength(String string, int maxLength) {
        return string.length() <= maxLength ? string : string.substring(0, maxLength);
    }

    private StringUtil() {
        throw new UnsupportedOperationException();
    }
}
