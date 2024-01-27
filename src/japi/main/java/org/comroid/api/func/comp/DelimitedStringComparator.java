package org.comroid.api.func.comp;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class DelimitedStringComparator implements Comparator<String> {
    public static final Comparator<String> INTEGER_COMPARATOR = Comparator.comparingInt(Integer::parseInt);
    public static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");
    private final Pattern delimiter;

    public DelimitedStringComparator(@Language("RegExp") String delimiter) {
        this.delimiter = Pattern.compile(delimiter);
    }

    private static int compareString(String left, int lengthL, String right, int lengthR, int index) {
        if (index >= lengthL || index >= lengthR)
            return lengthR - lengthL;

        char lc = left.charAt(index);
        char rc = right.charAt(index);

        if (lc == rc)
            return compareString(left, lengthL, right, lengthR, index + 1);

        return rc - lc;
    }

    @Override
    public int compare(String it, String other) {
        return Arrays.stream(delimiter.split(it))
                .map(StringBox::of)
                .mapToInt(box -> box.compareTo(other))
                .findFirst()
                .orElse(0);
    }

    public static final class StringBox implements Comparable<String> {
        private static final Map<String, StringBox> boxCache = new ConcurrentHashMap<>();
        private final String inside;

        private StringBox(String inside) {
            this.inside = inside;
        }

        public static Comparable<String> of(String string) {
            return boxCache.computeIfAbsent(string, StringBox::new);
        }

        @Override
        public int compareTo(@NotNull String other) {
            if (INTEGER_PATTERN.matcher(other).matches())
                return INTEGER_COMPARATOR.compare(inside, other);

            return compareString(inside.toLowerCase(), inside.length(), other.toLowerCase(), inside.length(), 0);
        }
    }
}
