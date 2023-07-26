package org.comroid.abstr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Delegate;
import org.comroid.api.Polyfill;
import org.comroid.api.Specifiable;
import org.comroid.api.ValueBox;
import org.comroid.api.ValueType;
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

    public <T> @Nullable T as(ValueType<T> type) {
        var node = asValue();
        var val = node.value;
        if (val == null)
            return null;
        if (!type.test(val))
            return type.parse(val.toString());
        return type.getTargetClass().cast(val);
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
        return Objects.requireNonNullElse(as(StandardValueType.BOOLEAN), fallback);
    }

    public byte asByte(byte fallback) {
        return Objects.requireNonNullElse(as(StandardValueType.BYTE), fallback);
    }

    public char asChar(char fallback) {
        return Objects.requireNonNullElse(as(StandardValueType.CHARACTER), fallback);
    }

    public short asShort(short fallback) {
        return Objects.requireNonNullElse(as(StandardValueType.SHORT), fallback);
    }

    public int asInt(int fallback) {
        return Objects.requireNonNullElse(as(StandardValueType.INTEGER), fallback);
    }

    public long asLong(long fallback) {
        return Objects.requireNonNullElse(as(StandardValueType.LONG), fallback);
    }

    public float asFloat(float fallback) {
        return Objects.requireNonNullElse(as(StandardValueType.FLOAT), fallback);
    }

    public double asDouble(double fallback) {
        return Objects.requireNonNullElse(as(StandardValueType.DOUBLE), fallback);
    }

    @Contract("null -> _; !null -> !null")
    public String asString(String fallback) {
        return Objects.requireNonNullElse(as(StandardValueType.STRING), fallback);
    }

    @Contract("null -> _; !null -> !null")
    public UUID asUUID(UUID fallback) {
        return Objects.requireNonNullElse(as(StandardValueType.UUID), fallback);
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
