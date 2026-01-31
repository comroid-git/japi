package org.comroid.api.data.seri.adp;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.comroid.annotations.Instance;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.Serializer;
import org.comroid.api.func.ext.Context;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public enum Jackson implements Serializer<JSON.Node> {
    @Instance JSON;

    @Override
    @SneakyThrows
    public @Nullable JSON.Node parse(@Language("JSON") @Nullable String data) {
        final var mapper = Context.root().getFromContext(ObjectMapper.class, true).assertion();

        //noinspection unchecked
        return data == null
               ? DataNode.Value.NULL.json()
               : data.trim().startsWith("{")
                 ? org.comroid.api.data.seri.adp.JSON.Object.of(mapper.readValue(data,
                       Map.class))
                 : data.trim().startsWith("[") ? org.comroid.api.data.seri.adp.JSON.Array.of(mapper.readValue(data,
                         List.class)) : DataNode.of(mapper.readTree(data)).json();
    }

    @Override
    public JSON.Node createObjectNode() {
        return new JSON.Object();
    }

    @Override
    public JSON.Node createArrayNode() {
        return new JSON.Array();
    }
}
