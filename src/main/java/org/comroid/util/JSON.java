package org.comroid.util;

import lombok.Data;
import lombok.SneakyThrows;
import org.comroid.abstr.DataNode;
import org.comroid.annotations.Instance;
import org.comroid.api.DelegateStream;
import org.comroid.api.MimeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

public enum JSON implements org.comroid.api.Serializer<DataNode> {
    @Instance Parser;

    @Override
    public MimeType getMimeType() {
        return MimeType.JSON;
    }

    @Override
    public @NotNull DataNode parse(@Nullable String data) {
        if (data == null)
            return Value.NULL;
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

    public static class Serializer extends DelegateStream.Output {
        public Serializer(OutputStream delegate) {
            super(delegate);
        }

        public Serializer(Writer delegate) {
            super(delegate);
        }

        @SneakyThrows
        public void write(DataNode dataNode) {
            write(StandardCharsets.US_ASCII.encode(dataNode.toString()).array());
        }
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
        public DataNode readNode() {
            return switch (getOrAdvance()) {
                case '{' -> readObject();
                case '[' -> readArray();
                default -> new Value<>(readToken());
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

    @Data
    public static final class Object extends DataNode.Object {
        @Override
        public String toString() {
            return map.entrySet().stream()
                    .map(e -> "\"%s\": %s".formatted(e.getKey(), e.getValue()))
                    .collect(Collectors.joining(", ", "{", "}"));
        }
    }

    @Data
    public static final class Array extends DataNode.Array {
        @Override
        public String toString() {
            return list.stream()
                    .map(Objects::toString)
                    .collect(Collectors.joining(", ", "[", "]"));
        }
    }

    @Data
    public static final class Value<T> extends DataNode.Value<T> {
        public Value() {
        }

        public Value(T value) {
            super(value);
        }

        @Override
        public String toString() {
            var str = String.valueOf(value);
            if (value instanceof String)
                return "\"%s\"".formatted(str);
            return str;
        }
    }
}
