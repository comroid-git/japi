package org.comroid.util;

import lombok.Data;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import org.comroid.annotations.Instance;
import org.comroid.api.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public enum JSON implements Serializer<JSON.Node> {
    @Instance Parser;
    public static final String MimeType = "application/json";

    @Override
    public CharSequence getMimeType() {
        return MimeType;
    }

    @Override
    public @NotNull JSON.Node parse(@Nullable String data) {
        if (data == null)
            return Node.Value.NULL;
        try (var reader = new Stream(new StringReader(data))) {
            return reader.readNode();
        }
    }

    @Override
    public Node.Object createObjectNode() {
        return new Node.Object();
    }

    @Override
    public Node.Array createArrayNode() {
        return new Node.Array();
    }

    public static class Stream extends DelegateStream.Input {
        private char c;

        public Stream(InputStream delegate) {
            super(delegate);
        }

        public Stream(Reader delegate) {
            super(delegate);
        }

        @SneakyThrows
        public Node readNode() {
            return switch (getOrAdvance()) {
                case '{' -> readObject();
                case '[' -> readArray();
                default -> {
                    //take();
                    yield new Node.Value<>(readToken());
                }
            };
        }

        @SneakyThrows
        public Node.Object readObject() {
            if (getOrAdvanceAndTake() != '{')
                throw err(c, '{', "start of object");
            var obj = new Node.Object();

            while (getOrAdvance() != '}') {
                if (getOrAdvance() == ',')
                    take();
                var key = readToken().toString();
                if (getOrAdvance() != ':')
                    throw err(this.c, ':', "key/value delimiter");
                take();
                var val = readNode();
                obj.map.put(key, val);
            }
            take();
            return obj;
        }

        @SneakyThrows
        public Node.Array readArray() {
            if (getOrAdvanceAndTake() != '[')
                throw err(c, '[', "start of array");
            var arr = new Node.Array();

            while (getOrAdvance() != ']') {
                if (getOrAdvance() == ',')
                    take();
                var val = readNode();
                arr.list.add(val);
            }
            take();
            return arr;
        }

        @SneakyThrows
        private Object readToken() {
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

        //private char expect(char c, boolean take) {}
        private char getOrAdvanceAndTake() {
            if (c == 0)
                advance();
            return take();
        }

        private char advanceAndTake() {
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
    public static abstract class Node implements Specifiable<Node> {
        public Object asObject() {
            return as(Object.class).assertion();
        }

        public Array asArray() {
            return as(Array.class).assertion();
        }

        public <T> Value<T> asValue() {
            return as(Value.class).map(Polyfill::<Value<T>>uncheckedCast).assertion();
        }

        public <T> T as(ValueType<T> type) {
            var node = asValue();
            var val = node.value;
            if (!type.test(val))
                return type.parse(val.toString());
            return type.getTargetClass().cast(val);
        }

        public boolean asBoolean() {
            return as(StandardValueType.BOOLEAN);
        }

        public byte asByte() {
            return as(StandardValueType.BYTE);
        }

        public char asChar() {
            return as(StandardValueType.CHARACTER);
        }

        public short asShort() {
            return as(StandardValueType.SHORT);
        }

        public int asInt() {
            return as(StandardValueType.INTEGER);
        }

        public long asLong() {
            return as(StandardValueType.LONG);
        }

        public float asFloat() {
            return as(StandardValueType.FLOAT);
        }

        public double asDouble() {
            return as(StandardValueType.DOUBLE);
        }

        public String asString() {
            return as(StandardValueType.STRING);
        }

        public UUID asUUID() {
            return as(StandardValueType.UUID);
        }

        @Data
        public static final class Value<T> extends Node implements ValueBox<T> {
            public static final Node NULL = new Value<>(null);
            private final T value;

            @Override
            public String toString() {
                if (value instanceof String)
                    return "\"%s\"".formatted(value);
                return value.toString();
            }
        }

        @Data
        public static final class Object extends Node implements Map<String, Node> {
            @Delegate
            private final Map<String, Node> map = new ConcurrentHashMap<>();

            @Override
            public String toString() {
                return map.entrySet().stream()
                        .map(e -> "\"%s\": %s".formatted(e.getKey(), e.getValue()))
                        .collect(Collectors.joining(", ", "{", "}"));
            }
        }

        @Data
        public static final class Array extends Node implements List<Node> {
            @Delegate
            private final List<Node> list = new ArrayList<>();

            @Override
            public String toString() {
                return list.stream()
                        .map(Objects::toString)
                        .collect(Collectors.joining(", ", "[", "]"));
            }
        }
    }
}
