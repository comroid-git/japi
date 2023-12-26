package org.comroid.api.data.seri;

import java.util.regex.Pattern;

public final class RegExpUtil {
    public static final String ANYTHING_PATTERN = ".*";
    public static final String UUID4_PATTERN = "\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b";
    public static final String INTEGER_PATTERN = "\\d+";
    public static final String DECIMAL_PATTERN = "\\d+(\\.\\d+)?[fd]?";
    public static final Pattern ANYTHING = Pattern.compile(ANYTHING_PATTERN);
    public static final Pattern UUID4 = Pattern.compile(UUID4_PATTERN);
    public static final Pattern INTEGER = Pattern.compile(INTEGER_PATTERN);
    public static final Pattern DECIMAL = Pattern.compile(DECIMAL_PATTERN);
}
