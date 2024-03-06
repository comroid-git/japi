package org.comroid.api.data.seri;

import org.comroid.annotations.Instance;
import org.comroid.api.data.seri.type.StandardValueType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum FormData implements Serializer<DataNode.Object> {
    @Instance Parser;

    @Override
    public MimeType getMimeType() {
        return MimeType.URLENCODED;
    }

    @Override
    public @Nullable FormData.Object parse(@Language(value = "http-url-reference", prefix = "https://comroid.org/?") @Nullable String data) {
        final var obj = new Object();
        if (data == null) return obj;
        Arrays.stream(data.split("&"))
                .filter(Predicate.not(String::isBlank))
                .map(p -> p.split("="))
                .forEach(p -> obj.set(p[0], p.length == 2 || !p[1].isBlank() ? StandardValueType.findGoodType(p[1]) : null));
        return obj;
    }

    @Override
    public FormData.Object createObjectNode() {
        return new Object();
    }

    @Override
    public DataNode.Object createArrayNode() {
        throw new UnsupportedOperationException("FormData cannot be an array");
    }

    public static final class Object extends DataNode.Object {
        @Override
        public String toString() {
            return map.entrySet()
                    .stream()
                    .filter(e -> !"null".equals(e.getValue().asString()))
                    .map(e -> {
                        var str = e.getValue().toString();
                        str = str.substring(1, str.length() - 1);
                        return "%s=%s".formatted(e.getKey(), str);
                    })
                    .collect(Collectors.joining("&"));
        }
    }
}
