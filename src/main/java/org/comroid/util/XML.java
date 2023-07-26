package org.comroid.util;

import lombok.Data;
import org.comroid.abstr.DataNode;
import org.comroid.annotations.Instance;
import org.comroid.api.DelegateStream;
import org.comroid.api.MimeType;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.io.Writer;

public enum XML implements org.comroid.api.Serializer<DataNode> {
    @Instance Parser;

    @Override
    public MimeType getMimeType() {
        return MimeType.XML;
    }

    @Override
    public @Nullable DataNode parse(@Nullable String data) {
        return null;
    }

    @Override
    public Node createObjectNode() {
        return new Node();
    }

    @Override
    public Node createArrayNode() {
        return new Node();
    }

    public static class Deserializer extends DelegateStream.Output {
        public Deserializer(OutputStream delegate) {
            super(delegate);
        }

        public Deserializer(Writer delegate) {
            super(delegate);
        }

        // todo: implement
    }

    @Data
    public static final class Node extends DataNode.Object {
        @Override
        public String toString() {
            var sb = new StringBuilder("<").append(name);
            if (map.size() > 0) {
                for (var entry : map.entrySet()) {
                    sb.append(' ').append(entry.getKey());
                    var node = entry.getValue();
                    if (node != null)
                        sb.append("=").append(node).append('"');
                }
            }
            if (children.isEmpty())
                sb.append(" />");
            else {
                sb.append('>');
                for (var node : children)
                    sb.append(node);
                sb.append("</").append(name).append('>');
            }
            return sb.toString();
        }
    }
}
