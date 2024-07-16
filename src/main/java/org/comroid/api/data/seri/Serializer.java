package org.comroid.api.data.seri;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface Serializer<T> extends Function<String, T>, MimeType.Container {

    default T apply(String string) {
        return parse(string);
    }

    @Nullable
    T parse(@Nullable String data);

    T createObjectNode();

    T createArrayNode();
}
