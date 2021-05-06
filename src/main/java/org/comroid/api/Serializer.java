package org.comroid.api;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface Serializer<T> extends Function<String, T> {
    CharSequence getMimeType();

    @Nullable T parse(@Nullable String data);

    default T apply(String string) {
        return parse(string);
    }

    T createObjectNode();

    T createArrayNode();
}
