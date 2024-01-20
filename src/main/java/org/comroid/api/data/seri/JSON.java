package org.comroid.api.data.seri;

import lombok.Data;
import lombok.SneakyThrows;
import org.comroid.annotations.Convert;
import org.comroid.annotations.Instance;
import org.comroid.api.func.util.DelegateStream;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public enum JSON implements Serializer<JSON.Node> {
    @Instance Parser;

    @Override
    public MimeType getMimeType() {
        return MimeType.JSON;
    }

    @Override
    public @NotNull JSON.Node parse(@Language("JSON") @Nullable String data) {
        if (data == null)
            return DataNode.Value.NULL.json();
        try (var reader = new Deserializer(new StringReader(data))) {
            return reader.readNode();
        }
    }

    @Override
    public Object createObjectNode() {
        return new Object();
    }

    @Override
    public Array createArrayNode() {
        return new Array();
    }

    public static class Deserializer extends DelegateStream.Input {
        private char c;

        public Deserializer(InputStream delegate) {
            super(delegate);
        }

        public Deserializer(Reader delegate) {
            super(delegate);
        }

        @SneakyThrows
        public JSON.Node readNode() {
            return switch (getOrAdvance()) {
                case '{' -> readObject();
                case '[' -> readArray();
                default -> new DataNode.Value<>(readToken()).json();
            };
        }

        @SneakyThrows
        public Object readObject() {
            if (getOrAdvanceAndTake() != '{')
                throw err(c, '{', "start of object");
            var obj = new Object();

            while (getOrAdvance() != '}') {
                if (getOrAdvance() == ',')
                    take();
                var key = readToken().toString();
                if (getOrAdvance() != ':')
                    throw err(this.c, ':', "key/value delimiter");
                take();
                var val = readNode();
                obj.put(key, val);
            }
            take();
            return obj;
        }

        @SneakyThrows
        public Array readArray() {
            if (getOrAdvanceAndTake() != '[')
                throw err(c, '[', "start of array");
            var arr = new Array();

            while (getOrAdvance() != ']') {
                if (getOrAdvance() == ',')
                    take();
                var val = readNode();
                arr.add(val);
            }
            take();
            return arr;
        }

        @SneakyThrows
        private java.lang.Object readToken() {
            // first char may have been read to buffer by previous call
            var c = getOrAdvanceAndTake();
            var numeric = Character.isDigit(c) || c == '-';
            var buf = new StringBuilder();
            if (numeric)
                buf.append(c);
            while (true) {
                c = advance();
                if (numeric
                        ? !Character.isDigit(c)
                        : c == '"')
                    break;
                else if (c == '\\')
                    buf.append(c = getAndAdvance());
                buf.append(c);
            }
            var str = buf.toString();
            if (!numeric) {
                take();
                return str;
            }
            return StandardValueType.findGoodType(str);
        }

        private char getOrAdvanceAndTake() {
            if (c == 0)
                advance();
            return take();
        }

        private char take() {
            var c = this.c;
            this.c = 0;
            return c;
        }

        private char getAndAdvance() {
            var c = this.c;
            advance();
            return c;
        }

        private char getOrAdvance() {
            if (c == 0)
                advance();
            return c;
        }

        @SneakyThrows
        private char advance() {
            int c;
            do {
                c = read();
                if (c == -1)
                    throw new IOException("unexpected end of stream");
            } while (Character.isWhitespace(c) || c == '\n' || c == '\r' || c == '\t');
            return this.c = (char) c;
        }

        private IOException err(char actual, char expected, String where) {
            return new IOException("invalid char '" + actual + "' at " + where + "; expected '" + expected + "'");
        }
    }

    public interface Node extends DataNode {
        @Override
        default Node json() {
            return this;
        }

        @Override
        default MimeType getMimeType() {
            return MimeType.JSON;
        }
    }

    @Data
    public static final class Object extends DataNode.Object implements Node {
        public static JSON.Object of(Map<String, java.lang.Object> map) {
            var obj = new JSON.Object();
            for (var entry : map.entrySet())
                obj.put(entry.getKey(), DataNode.of(entry.getValue()));
            return obj;
        }

        @Override
        public String toString() {
            return map.entrySet().stream()
                    .map(e -> "\"%s\": %s".formatted(e.getKey(), e.getValue().toString()))
                    .collect(Collectors.joining(", ", "{", "}"));
        }
    }

    @Data
    public static final class Array extends DataNode.Array implements Node {
        public static JSON.Array of(List<java.lang.Object> list) {
            var arr = new JSON.Array();
            for (var each : list)
                arr.add(DataNode.of(each));
            return arr;
        }

        @Override
        public String toString() {
            return list.stream()
                    .map(Objects::toString)
                    .collect(Collectors.joining(", ", "[", "]"));
        }
    }

    @Data
    public static final class Value<T> extends DataNode.Value<T> implements Node {
        public Value(@Nullable T value) {
            super(value);
        }

        @Override
        public String toString() {
            // this method is a super-stub because it depends on the functionality of the underlying method
            return super.toString();
        }

        @Convert
        public static <T> JSON.Value<T> convert(Value<T> from) {
            if (from instanceof JSON.Value)
                return (JSON.Value<T>) from;
            return new JSON.Value<>(from.getValue());
        }
    }
}
