package org.comroid.api;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public interface TextDecoration extends UnaryOperator<CharSequence>, Predicate<CharSequence>, WrappedFormattable {
    CharSequence getPrefix();
    CharSequence getSuffix();

    @Override
    default CharSequence apply(CharSequence seq) {
        return String.valueOf(getPrefix())+seq+getSuffix();
    }

    @Override
    default boolean test(CharSequence seq) {
        final var str = seq.toString();
        var i = str.indexOf(getPrefix().toString());
        return i != -1 && str.indexOf(getSuffix().toString(), i) != -1;
    }
}
