package org.comroid.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.comroid.abstr.DataNode;
import org.comroid.annotations.Instance;
import org.comroid.api.Serializer;
import org.comroid.util.JSON;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public enum Jackson implements Serializer<DataNode> {
    @Instance JSON;

    @Override
    @SneakyThrows
    public @Nullable DataNode parse(@Nullable String data) {
        //noinspection unchecked
        return data == null ? DataNode.of(null)
                : data.trim().startsWith("{") ? org.comroid.util.JSON.Object.of(new ObjectMapper().convertValue(data, Map.class))
                : data.trim().startsWith("[") ? org.comroid.util.JSON.Array.of(new ObjectMapper().convertValue(data, List.class))
                : DataNode.of(new ObjectMapper().readTree(data));
    }

    @Override
    public DataNode createObjectNode() {
        return new JSON.Object();
    }

    @Override
    public DataNode createArrayNode() {
        return new JSON.Array();
    }
}
