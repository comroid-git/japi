package org.comroid.util;

import org.jetbrains.annotations.ApiStatus.Experimental;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.function.Function;

@Experimental
public final class StringBasedComparator<T> implements Comparator<T> {
    private final Function<T, String> toStringFunction;

    public StringBasedComparator() {
        this(String::valueOf);
    }

    public StringBasedComparator(Function<T, String> toStringFunction) {
        this.toStringFunction = toStringFunction;
    }

    @Override
    public int compare(T it, T other) {
        byte[] ic = toStringFunction.apply(it)
                .toLowerCase()
                .getBytes(StandardCharsets.US_ASCII);
        byte[] oc = toStringFunction.apply(other)
                .toLowerCase()
                .getBytes(StandardCharsets.US_ASCII);

        long iv = 0;
        long ov = 0;

        for (int i = 0; i < ic.length && i < oc.length; i++) {
            byte icp = ic[i];
            byte ocp = oc[i];

            // if codepoints are equal, skip
            if (icp == ocp)
                continue;

            boolean il = Character.isLetter(icp) || Character.isDigit(icp);
            boolean ol = Character.isLetter(ocp) || Character.isDigit(ocp);
            // if none are letter or digit, skip
            if (!il && !ol)
                continue;
            // if other codepoint is not letter or digit, 'it' is larger
            if (il && !ol)
                return -1;
            // if it's codepoint is not letter or digit, other is larger
            if (!il)
                return 1;

            if (iv != 0 || ov != 0) {
                iv <<= 6;
                ov <<= 6;
            }
            iv |= icp;
            ov |= ocp;
        }

        // if checksums are equal, use length
        return iv == ov
                ? ic.length - oc.length
                : (int) (iv - ov);
    }
}
