package org.comroid.abstr;

import lombok.*;
import lombok.experimental.Delegate;
import org.comroid.api.*;
import org.comroid.util.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.comroid.util.StandardValueType.*;

public interface DataNode extends Specifiable<DataNode> {
    default DelegateStream.Input toInputStream() {
        return new DelegateStream.Input(new StringReader(toString()));
    }

    default DataNode json() {
        if (this instanceof Object) {
            if (this instanceof JSON.Object)
                return this;
            else return properties().collect(JSON.Object::new, (n, e) -> n.put(e.key, e.node()), Map::putAll);
        } else if (this instanceof Array) {
            if (this instanceof JSON.Array)
                return this;
            else return properties().collect(JSON.Array::new, (n, e) -> n.add(e.node()), List::addAll);
        }
        return of(this).json();
    }

    default FormData.Object form() {
        if (this instanceof Object) {
            if (this instanceof FormData.Object)
                return (FormData.Object) this;
            else return properties().collect(FormData.Object::new, (n, e) -> n.put(e.key, e.node()), Map::putAll);
        }
        return of(this).form();
    }

    default Object asObject() {
        return as(Object.class).assertion();
    }

    default Array asArray() {
        return as(Array.class).assertion();
    }

    default <T> Value<T> asValue() {
        return as(Value.class).map(Polyfill::<Value<T>>uncheckedCast).assertion();
    }

    default int size() {
        return (int) properties().count();
    }

    @NotNull
    default DataNode get(java.lang.Object key) {
        return Objects.requireNonNullElse(asObject().get(String.valueOf(key)), Value.NULL);
    }

    @NotNull
    default DataNode get(int index) {
        return Objects.requireNonNullElse(asArray().get(index), Value.NULL);
    }

    default <T> Rewrapper<T> as(ValueType<T> type) {
        var node = asValue();
        var val = node.value;
        if (val == null)
            return null;
        if (!type.test(val))
            return Rewrapper.of(type.parse(val.toString()));
        return Rewrapper.of(type.getTargetClass().cast(val));
    }

    default boolean asBoolean() {
        return asBoolean(false);
    }

    default byte asByte() {
        return asByte((byte) 0);
    }

    default char asChar() {
        return asChar((char) 0);
    }

    default short asShort() {
        return asShort((short) 0);
    }

    default int asInt() {
        return asInt(0);
    }

    default long asLong() {
        return asLong(0L);
    }

    default float asFloat() {
        return asFloat(0f);
    }

    default double asDouble() {
        return asDouble(0d);
    }

    @Nullable
    default String asString() {
        return asString(null);
    }

    @Nullable
    default UUID asUUID() {
        return asUUID(null);
    }

    default boolean asBoolean(boolean fallback) {
        return as(BOOLEAN).orElse(fallback);
    }

    default byte asByte(byte fallback) {
        return as(BYTE).orElse(fallback);
    }

    default char asChar(char fallback) {
        return as(CHARACTER).orElse(fallback);
    }

    default short asShort(short fallback) {
        return as(SHORT).orElse(fallback);
    }

    default int asInt(int fallback) {
        return as(INTEGER).orElse(fallback);
    }

    default long asLong(long fallback) {
        return as(LONG).orElse(fallback);
    }

    default float asFloat(float fallback) {
        return as(FLOAT).orElse(fallback);
    }

    default double asDouble(double fallback) {
        return as(DOUBLE).orElse(fallback);
    }

    @Contract("null -> _; !null -> !null")
    default String asString(String fallback) {
        return as(STRING).orElse(fallback);
    }

    @Contract("null -> _; !null -> !null")
    default UUID asUUID(UUID fallback) {
        return as(UUID).orElse(fallback);
    }

    default Stream<Entry> properties() {
        return properties(this);
    }

    static Stream<Entry> properties(final java.lang.Object it) {
        if (it instanceof DataNode.Base)
            return ((DataNode) it).properties();
        final var get = "get";
        return Stream.concat(
                Stream.of(it.getClass().getFields())
                        .filter(mtd -> !Modifier.isStatic(mtd.getModifiers()))
                        .filter(fld -> fld.canAccess(it))
                        .map(fld -> new Entry(fld.getName(), ThrowingSupplier.rethrowing(() -> of(fld.get(it))))),
                Stream.of(it.getClass().getMethods())
                        .filter(mtd -> !Modifier.isStatic(mtd.getModifiers()))
                        .filter(mtd -> mtd.canAccess(it))
                        .filter(mtd -> mtd.getName().startsWith(get) && mtd.getName().length() > get.length())
                        .filter(mtd -> mtd.getParameterCount() == 0)
                        .map(mtd -> {
                            if ("getClass".equals(mtd.getName()))
                                return new Entry("dtype", () -> new Value<>(StackTraceUtils.lessSimpleDetailedName(it.getClass())));
                            else {
                                var name = mtd.getName().substring(get.length());
                                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                                return new Entry(name, () -> of(Invocable.ofMethodCall(it, mtd).invokeRethrow()));
                            }
                        })
        );
    }

    static DataNode of(java.lang.Object it) {
        if (it == null)
            return Value.NULL;
        else if (it instanceof DataNode.Base)
            return (DataNode) it;
        else if (it instanceof Iterable) {
            // handle as array node
            var arr = new Array();
            ((Iterable<?>) it).iterator().forEachRemaining(arr::append);
            return arr;
        } else {
            var typeOf = typeOf(it);
            if (typeOf != null && !typeOf.equals(OBJECT))
                return new Value<>(it);
            var obj = new Object();
            properties(it).forEach(e -> obj.put(e.key, e.node()));
            return obj;
        }
    }

    @Data
    abstract class Base implements DataNode {
        protected final List<DataNode> children = new ArrayList<>();
    }

    class Serializer extends DelegateStream.Output {
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

    @Data
    class Object extends Base implements Convertible, Map<String, DataNode> {
        @Delegate
        protected final Map<String, DataNode> map = new ConcurrentHashMap<>();

        public <T> DataNode set(String key, T value) {
            var val = DataNode.of(value);
            put(key, val);
            return val;
        }

        @Override
        public Stream<DataNode.Entry> properties() {
            return entrySet().stream().map(e -> new DataNode.Entry(e.getKey(), () -> e.getValue() ));
        }

        @Override
        public <R> @NotNull R convert(Class<? super R> target) {
            return Rewrapper.of(Activator.get(target).createInstance(this)).cast();
        }
    }

    @Data
    class Array extends Base implements List<DataNode> {
        @Delegate
        protected final List<DataNode> list = new ArrayList<>();

        public <T> DataNode append(T value) {
            var val = DataNode.of(value);
            add(val);
            return val;
        }

        @Override
        public Stream<Entry> properties() {
            var ls = new ArrayList<Entry>();
            for (var i = 0; i < this.size(); i++) {
                final var fi = i;
                ls.add(new Entry(String.valueOf(i), () -> get(fi)));
            }
            return ls.stream();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class Value<T> extends Base implements ValueBox<T> {
        public static final DataNode NULL = new Value<>(null);
        protected @Nullable T value;

        @Override
        public int size() {
            return isNull() ? 0 : 1;
        }

        @Override
        public Stream<Entry> properties() {
            return Stream.of(new Entry("", ()->this));
        }

        @Override
        public String toString() {
            var str = String.valueOf(value);
            if (value instanceof String)
                return "\"%s\"".formatted(str);
            return str;
        }
    }

    class Entry implements Map.Entry<String, Rewrapper<DataNode>> {
        private final String key;
        private Rewrapper<DataNode> value;

        public Entry(String key, Rewrapper<DataNode> value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Rewrapper<DataNode> getValue() {
            return Polyfill.uncheckedCast(value);
        }

        @Override
        public Rewrapper<DataNode> setValue(Rewrapper<DataNode> value) {
            var prev = this.value;
            this.value = value;
            return Polyfill.uncheckedCast(prev);
        }

        public DataNode node() {
            final var it = value.get();
            return Optional.ofNullable(StandardValueType.typeOf(it))
                    .<DataNode>map(x -> new Value<>(it))
                    .orElseGet(() -> DataNode.of(it));
        }
    }
}