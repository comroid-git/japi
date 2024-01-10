package org.comroid.api.data.seri;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface Serializer<T> extends Function<String, T>, MimeType.Container {

    @Nullable T parse(@Nullable String data);

    default T apply(String string) {
        return parse(string);
    }

    T createObjectNode();

    T createArrayNode();
}
