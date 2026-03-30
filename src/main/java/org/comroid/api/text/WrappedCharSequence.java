package org.comroid.api.text;

import org.jspecify.annotations.NonNull;

public interface WrappedCharSequence extends CharSequence {
    @Override
    default int length() {
        return toString().length();
    }

    @Override
    default char charAt(int index) {
        return toString().charAt(index);
    }

    @Override
    @NonNull
    default CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }
}
