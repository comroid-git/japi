package org.comroid.api.data.seri;

import org.jetbrains.annotations.Nullable;

public enum StringValueSerializer implements Serializer<DataNode.Value<String>> {
    INSTANCE;

    @Override
    public MimeType getMimeType() {
        return MimeType.PLAIN;
    }

    @Override
    public DataNode.Value<String> parse(@Nullable String data) {
        return new DataNode.Value<>(data);
    }

    @Override
    public DataNode.Value<String> createObjectNode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataNode.Value<String> createArrayNode() {
        throw new UnsupportedOperationException();
    }
}
