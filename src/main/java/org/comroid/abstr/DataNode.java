package org.comroid.abstr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Delegate;
import org.comroid.api.*;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public abstract class DataNode implements Specifiable<DataNode> {
    public Object asObject() {
        return as(Object.class).assertion();
    }

    public Array asArray() {
        return as(Array.class).assertion();
    }

    public <T> Value<T> asValue() {
        return as(Value.class).map(Polyfill::<Value<T>>uncheckedCast).assertion();
    }

    public abstract int size();

    public @NotNull DataNode get(String key) {
        return Objects.requireNonNullElse(asObject().get(key), Value.NULL);
    }

    public @NotNull DataNode get(int index) {
        return Objects.requireNonNullElse(asArray().get(index), Value.NULL);
    }

    public <T> Rewrapper<T> as(ValueType<T> type) {
        var node = asValue();
        var val = node.value;
        if (val == null)
            return null;
        if (!type.test(val))
            return Rewrapper.of(type.parse(val.toString()));
        return Rewrapper.of(type.getTargetClass().cast(val));
    }

    public boolean asBoolean() {
        return asBoolean(false);
    }

    public byte asByte() {
        return asByte((byte) 0);
    }

    public char asChar() {
        return asChar((char) 0);
    }

    public short asShort() {
        return asShort((short) 0);
    }

    public int asInt() {
        return asInt(0);
    }

    public long asLong() {
        return asLong(0L);
    }

    public float asFloat() {
        return asFloat(0f);
    }

    public double asDouble() {
        return asDouble(0d);
    }

    public @Nullable String asString() {
        return asString(null);
    }

    public @Nullable UUID asUUID() {
        return asUUID(null);
    }

    public boolean asBoolean(boolean fallback) {
        return as(StandardValueType.BOOLEAN).orElse(fallback);
    }

    public byte asByte(byte fallback) {
        return as(StandardValueType.BYTE).orElse(fallback);
    }

    public char asChar(char fallback) {
        return as(StandardValueType.CHARACTER).orElse(fallback);
    }

    public short asShort(short fallback) {
        return as(StandardValueType.SHORT).orElse(fallback);
    }

    public int asInt(int fallback) {
        return as(StandardValueType.INTEGER).orElse(fallback);
    }

    public long asLong(long fallback) {
        return as(StandardValueType.LONG).orElse(fallback);
    }

    public float asFloat(float fallback) {
        return as(StandardValueType.FLOAT).orElse(fallback);
    }

    public double asDouble(double fallback) {
        return as(StandardValueType.DOUBLE).orElse(fallback);
    }

    @Contract("null -> _; !null -> !null")
    public String asString(String fallback) {
        return as(StandardValueType.STRING).orElse(fallback);
    }

    @Contract("null -> _; !null -> !null")
    public UUID asUUID(UUID fallback) {
        return as(StandardValueType.UUID).orElse(fallback);
    }

    @Data
    public abstract static class Object extends DataNode implements Map<String, DataNode> {
        @Delegate
        protected final Map<String, DataNode> map = new ConcurrentHashMap<>();
    }

    @Data
    public abstract static class Array extends DataNode implements List<DataNode> {
        @Delegate
        protected final List<DataNode> list = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public abstract static class Value<T> extends DataNode implements ValueBox<T> {
        public static final DataNode NULL = new Value<>(null) {
        };
        protected @Nullable T value;

        @Override
        public int size() {
            return isNull() ? 0 : 1;
        }
    }
}
