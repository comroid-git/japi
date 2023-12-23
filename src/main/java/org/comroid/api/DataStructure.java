package org.comroid.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.comroid.annotations.Alias;
import org.comroid.annotations.Convert;
import org.comroid.annotations.Ignore;
import org.comroid.annotations.Instance;
import org.comroid.api.map.WeakCache;
import org.comroid.util.BoundValueType;
import org.comroid.util.StandardValueType;
import org.comroid.util.Switch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.comroid.api.Polyfill.uncheckedCast;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DataStructure<T> implements Named {
    private static final WeakCache<Key<?>, DataStructure<?>> $cache = new WeakCache<>(DataStructure::create);
    public static final Map<Key<?>, DataStructure<?>> cache = Collections.unmodifiableMap($cache);

    @NotNull Class<? super T> type;
    @NotNull @ToString.Exclude List<Constructor> constructors = new ArrayList<>();
    @NotNull @ToString.Exclude Map<String, Property<?>> properties = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return type.getCanonicalName();
    }

    private <V> Property<V> createProperty(Class<V> type, String name, AnnotatedElement it) {
        var vt = StandardValueType.forClass(type).orElseGet(() -> BoundValueType.of(type));
        var getter = new Switch<AnnotatedElement, Invocable<V>>()
                .option(Field.class::isInstance, () -> Invocable.ofFieldGet((Field) it))
                .option(Method.class::isInstance, () -> Invocable.ofMethodCall((Method) it))
                .apply(it);
        var setter = new Switch<AnnotatedElement, Invocable<?>>()
                .option(Field.class::isInstance, () -> Invocable.ofFieldSet((Field) it))
                .option(Method.class::isInstance, () -> Arrays.stream(((Method) it).getDeclaringClass().getMethods())
                        .filter(mtd -> Modifier.isPublic(mtd.getModifiers()) && !Modifier.isStatic(mtd.getModifiers()))
                        .filter(mtd -> mtd.getParameterCount() == 1 && ((Method) it).getReturnType().isAssignableFrom(mtd.getParameters()[0].getType()))
                        .filter(mtd -> mtd.getName().equalsIgnoreCase("set"+name))
                        .findAny()
                        .map(Invocable::ofMethodCall)
                        .orElse(null))
                .apply(it);
        var prop = new Property<V>(uncheckedCast(vt), name, getter, setter);
        var aliases = it.getAnnotation(Alias.class);
        if (aliases != null) prop.aliases.addAll(Arrays.asList(aliases.value()));
        return prop;
    }

    @lombok.Builder
    public static <T> DataStructure<T> of(@NotNull Class<? super T> target, @NotNull Class<? super T> above) {
        return uncheckedCast($cache.touch(new Key<T>(target, above)));
    }

    private static <T> DataStructure<T> create(Key<T> key) {
        final var target = key.type;
        final var struct = new DataStructure<T>(target);

        // constructors
        Stream.concat(Arrays.stream(target.getMethods())
                                .filter(mtd -> Modifier.isStatic(mtd.getModifiers()))
                                .filter(mtd -> Stream.of(Instance.class,Convert.class)
                                        .anyMatch(mtd::isAnnotationPresent))
                                .filter(mtd -> target.isAssignableFrom(mtd.getReturnType())),
                        Arrays.stream(target.getConstructors())
                                .filter(it -> !it.isAnnotationPresent(Ignore.class)
                                        || !Arrays.asList(it.getAnnotation(Ignore.class).value()).contains(DataStructure.class)))
                .filter(it -> !key.above.equals(it.getDeclaringClass()) && key.above.isAssignableFrom(it.getDeclaringClass()))
                .filter(it -> Modifier.isPublic(it.getModifiers()))
                .map(it -> {
                    Invocable<T> func;
                    if (it instanceof java.lang.reflect.Constructor<?>)
                        func = Invocable.ofConstructor(uncheckedCast(it));
                    else if (it instanceof Method)
                        func = Invocable.ofMethodCall((Method) it);
                    else throw new AssertionError();
                    var ctor = struct.new Constructor(it.getName(), func);
                    for (var parameter : it.getParameters())
                        ctor.args.put(parameter.getName(), struct.createProperty(parameter.getType(), parameter.getName(), parameter));
                    return ctor;
                }).forEach(struct.constructors::add);

        // properties
        Stream.concat(Arrays.stream(target.getFields()),
                        Arrays.stream(target.getMethods())
                                .filter(mtd -> mtd.getParameterCount() == 0)
                                .filter(mtd -> mtd.getName().startsWith("get")))
                .filter(it -> !Modifier.isStatic(it.getModifiers()))
                .filter(it -> Modifier.isPublic(it.getModifiers()))
                .filter(it -> !it.isAnnotationPresent(Ignore.class)
                        || !Arrays.asList(it.getAnnotation(Ignore.class).value()).contains(DataStructure.class))
                .filter(it -> !key.above.equals(it.getDeclaringClass()) && key.above.isAssignableFrom(it.getDeclaringClass()))
                .map(it -> {
                    var type = new Switch<Member, Class<?>>()
                            .option(Field.class::isInstance, () -> ((Field) it).getType())
                            .option(Method.class::isInstance, () -> ((Method) it).getReturnType())
                            .apply(it);
                    var name = it.getName();
                    if (it instanceof Method)
                        name = Character.toLowerCase(name.charAt(3)) + name.substring("getX".length());
                    return struct.createProperty(type, name, it);
                })
                .forEach(prop -> struct.properties.put(prop.name, prop));

        return struct;
    }

    @Value
    public class Constructor implements Named {
        @Nullable String name;
        @NotNull @ToString.Exclude Map<String, Property<?>> args = new ConcurrentHashMap<>();
        @NotNull @ToString.Exclude @Getter(onMethod = @__(@JsonIgnore)) Invocable<T> func;
    }

    @Value
    public static class Property<V> implements Named {
        @NotNull ValueType<V> type;
        @NotNull String name;
        @Nullable @ToString.Exclude @Getter(onMethod = @__(@JsonIgnore)) Invocable<V> getter;
        @Nullable @ToString.Exclude @Getter(onMethod = @__(@JsonIgnore)) Invocable<?> setter;
        @NotNull Set<String> aliases = new HashSet<>();

        @Value
        public class Usage {
            @Nullable V value;

            public Property<V> getProperty() {
                return Property.this; // for serialization
            }
        }
    }

    @Value
    public static class Key<T> {
        @NotNull Class<? super T> type;
        @NotNull Class<? super T> above;
    }
}
