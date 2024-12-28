package org.comroid.api.data.seri;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import org.comroid.annotations.Ignore;
import org.comroid.api.Polyfill;
import org.comroid.api.data.bind.DataStructure;
import org.comroid.api.data.seri.adp.FormData;
import org.comroid.api.data.seri.adp.JSON;
import org.comroid.api.data.seri.type.StandardValueType;
import org.comroid.api.data.seri.type.ValueType;
import org.comroid.api.func.Specifiable;
import org.comroid.api.func.ValueBox;
import org.comroid.api.func.ext.Convertible;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.DelegateStream;
import org.comroid.api.info.Constraint;
import org.comroid.api.java.Activator;
import org.comroid.api.java.StackTraceUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static org.comroid.api.data.seri.type.StandardValueType.*;

@Ignore({ Convertible.class, DataStructure.class })
public interface DataNode extends MimeType.Container, StringSerializable, Specifiable<Object> {
    static Stream<Entry> properties(final java.lang.Object it) {
        if (it instanceof DataNode.Base)
            return ((DataNode) it).properties();
        if (it instanceof Map<?, ?> map)
            return map.entrySet().stream().collect(new Collector<Map.Entry<?, ?>, List<Entry>, Stream<Entry>>() {
                @Override
                public Supplier<List<Entry>> supplier() {
                    return ArrayList::new;
                }

                @Override
                public BiConsumer<List<Entry>, Map.Entry<?, ?>> accumulator() {
                    return (ls, e) -> ls.add(new Entry(String.valueOf(e.getKey()), of(e.getValue())));
                }

                @Override
                public BinaryOperator<List<Entry>> combiner() {
                    return (l, r) -> {
                        l.addAll(r);
                        return l;
                    };
                }

                @Override
                public Function<List<Entry>, Stream<Entry>> finisher() {
                    return Collection::stream;
                }

                @Override
                public Set<Characteristics> characteristics() {
                    return Set.of();
                }
            });
        return DataStructure.of(it.getClass(), java.lang.Object.class)
                .getDeclaredProperties().values().stream()
                .map(Polyfill::<DataStructure<java.lang.Object>.Property<Object>>uncheckedCast)
                .map(prop -> new Entry(prop.getName(), of(prop.getFrom(it))));
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
            // handle as object node
            var typeOf = typeOf(it);
            if (typeOf instanceof StandardValueType<?>)
                return new Value<>(it);
            var obj = new Object();
            properties(it).forEach(e -> obj.put(e.key, e.getValue()));
            return obj;
        }
    }

    @Override
    @JsonIgnore
    default MimeType getMimeType() {
        final var supported = Map.of(
                "json", MimeType.JSON,
                "form", MimeType.URLENCODED
        );
        return StackTraceUtils.stream()
                .map(StackTraceElement::getMethodName)
                .map(name -> supported.getOrDefault(name, null))
                .filter(Objects::nonNull)
                .findFirst()
                //.orElseThrow(()->new AbstractMethodError("MimeType was not specified or supported for DataNode " + this));
                .orElse(MimeType.JSON);
    }

    @Override
    default String toSerializedString() {
        return this instanceof JSON.Node ? toString() : json().toSerializedString();
    }

    default DelegateStream.Input toInputStream() {
        return new DelegateStream.Input(new StringReader(toString()));
    }

    default JSON.Node json() {
        if (this instanceof JSON.Node)
            return (JSON.Node) this;
        else if (this instanceof Object)
            return properties().collect(JSON.Object::new, (n, e) -> n.put(e.key, e.getValue().json()), Map::putAll);
        else if (this instanceof Array)
            return properties().collect(JSON.Array::new, (n, e) -> n.add(e.getValue().json()), List::addAll);
        else if (this instanceof Value)
            return Convertible.convert(this, JSON.Value.class);
        else return of(this).json();
    }

    default FormData.Object form() {
        if (this instanceof Object) {
            if (this instanceof FormData.Object)
                return (FormData.Object) this;
            else return properties().collect(FormData.Object::new, (n, e) -> n.put(e.key, e.getValue()), Map::putAll);
        }
        return of(this).form();
    }

    default int size() {
        return (int) properties().count();
    }

    @NotNull
    default DataNode get(java.lang.Object key) {
        return Objects.requireNonNullElse(asObject().get(String.valueOf(key)), Value.NULL);
    }

    default Object asObject() {
        return asType(Object.class);
    }

    default <T extends DataNode, R extends T> R asType(Class<? extends T> type) {
        Constraint.Type.anyOf(this, "type", type)
                .setConstraint("preliminary cast check").run();
        return Polyfill.uncheckedCast(type.cast(this));
    }

    @NotNull
    default DataNode get(int index) {
        return Objects.requireNonNullElse(asArray().get(index), Value.NULL);
    }

    default Array asArray() {
        return asType(Array.class);
    }

    default boolean asBoolean() {
        return asBoolean(false);
    }

    default boolean asBoolean(boolean fallback) {
        return as(BOOLEAN).orElse(fallback);
    }

    default <T> Wrap<T> as(ValueType<T> type) {
        var node = asValue();
        var val = node.value;
        if (val == null)
            return null;
        if (!type.test(val))
            return Wrap.of(type.parse(val.toString()));
        return Wrap.of(type.getTargetClass().cast(val));
    }

    default <T> Value<T> asValue() {
        return asType(Value.class);
    }

    default byte asByte() {
        return asByte((byte) 0);
    }

    default byte asByte(byte fallback) {
        return as(BYTE).orElse(fallback);
    }

    default char asChar() {
        return asChar((char) 0);
    }

    default char asChar(char fallback) {
        return as(CHARACTER).orElse(fallback);
    }

    default short asShort() {
        return asShort((short) 0);
    }

    default short asShort(short fallback) {
        return as(SHORT).orElse(fallback);
    }

    default int asInt() {
        return asInt(0);
    }

    default int asInt(int fallback) {
        return as(INTEGER).orElse(fallback);
    }

    default long asLong() {
        return asLong(0L);
    }

    default long asLong(long fallback) {
        return as(LONG).orElse(fallback);
    }

    default float asFloat() {
        return asFloat(0f);
    }

    default float asFloat(float fallback) {
        return as(FLOAT).orElse(fallback);
    }

    default double asDouble() {
        return asDouble(0d);
    }

    default double asDouble(double fallback) {
        return as(DOUBLE).orElse(fallback);
    }

    @Nullable
    default UUID asUUID() {
        return asUUID(null);
    }

    @Contract("null -> _; !null -> !null")
    default UUID asUUID(UUID fallback) {
        return as(UUID).orElse(fallback);
    }

    @Override
    default <R> Wrap<R> as(final Class<R> type) {
        return Specifiable.super.as(type)
                .orRef(() -> Wrap.of(ValueType.of(type))
                        .flatMap(StandardValueType.class)
                        .map(svt -> Polyfill.<R>uncheckedCast(svt.parse(asString())))
                        .or(() -> Activator.get(type).createInstance(this)));
    }

    @Nullable
    default String asString() {
        return asString(null);
    }

    @Contract("null -> _; !null -> !null")
    default String asString(String fallback) {
        return as(STRING).orElse(fallback);
    }

    default Stream<Entry> properties() {
        return properties(this);
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
            return entrySet().stream().map(e -> new DataNode.Entry(e.getKey(), e.getValue()));
        }

        @Override
        public <R> @Nullable R convert(Class<? super R> target) {
            return Wrap.of(Activator.get(target).createInstance(this)).cast();
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
                ls.add(new Entry(String.valueOf(i), get(fi)));
            }
            return ls.stream();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Ignore(Convertible.class)
    class Value<T> extends Base implements ValueBox<T> {
        public static final DataNode NULL = new Value<>(null) {
            @Override
            public ValueType<?> getHeldType() {
                return VOID;
            }
        };
        protected @Nullable T        value;

        @Override
        public int size() {
            return isNull() ? 0 : 1;
        }

        @Override
        public final Stream<Entry> properties() {
            return Stream.of(new Entry("", this));
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public String toSerializedString() {
            var str = String.valueOf(value);
            if (value instanceof String)
                return "\"%s\"".formatted(str);
            return str;
        }
    }

    class Entry implements Map.Entry<String, DataNode> {
        private final String   key;
        private       DataNode value;

        public Entry(String key, DataNode value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public DataNode getValue() {
            return value;
        }

        @Override
        public DataNode setValue(DataNode value) {
            var prev = this.value;
            this.value = value;
            return prev;
        }
    }
}
