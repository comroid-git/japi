package org.comroid.api;

import java.util.function.UnaryOperator;

public interface TextDecoration extends UnaryOperator<CharSequence> {
    CharSequence getPrefix();
    CharSequence getSuffix();

    @Override
    default CharSequence apply(CharSequence seq) {
        return String.valueOf(getPrefix())+seq+getSuffix();
    }
}
