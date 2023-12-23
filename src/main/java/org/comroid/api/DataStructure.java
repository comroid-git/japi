package org.comroid.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.comroid.annotations.Alias;
import org.comroid.annotations.Convert;
import org.comroid.annotations.Ignore;
import org.comroid.annotations.Instance;
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
    private static final Map<Class<?>, DataStructure<?>> $cache = new ConcurrentHashMap<>();
    public static final Map<Class<?>, DataStructure<?>> cache = Collections.unmodifiableMap($cache);

    Class<? super T> type;
    List<Constructor> constructors = new ArrayList<>();
    Map<String, Property<?>> properties = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return type.getCanonicalName();
    }

    private <V> Property<V> createProperty(Class<V> type, String name, @Nullable Alias aliases) {
        var vt = StandardValueType.forClass(type).orElseGet(() -> BoundValueType.of(type));
        var prop = new Property<V>(uncheckedCast(vt), name);
        if (aliases != null)
            prop.aliases.addAll(Arrays.asList(aliases.value()));
        return prop;
    }

    public static <T> DataStructure<T> of(final Class<? super T> target) {
        return uncheckedCast($cache.computeIfAbsent(target, $ -> {
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
                            ctor.args.put(parameter.getName(), struct.createProperty(parameter.getType(), parameter.getName(), parameter.getAnnotation(Alias.class)));
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
                    .map(it -> {
                        var type = new Switch<Member, Class<?>>()
                                .option(Field.class::isInstance, () -> ((Field) it).getType())
                                .option(Method.class::isInstance, () -> ((Method) it).getReturnType())
                                .apply(it);
                        var name = it.getName();
                        if (it instanceof Method)
                            name = Character.toLowerCase(name.charAt(3)) + name.substring("getX".length());
                        return struct.createProperty(type, name, it.getAnnotation(Alias.class));
                    })
                    .forEach(prop -> struct.properties.put(prop.name, prop));

            return struct;
        }));
    }

    @Value
    public class Constructor implements Named {
        @Nullable String name;
        @NotNull Map<String, Property<?>> args = new ConcurrentHashMap<>();
        @NotNull @Getter(onMethod = @__(@JsonIgnore)) Invocable<T> func;
    }

    @Value
    public static class Property<V> implements Named {
        @NotNull ValueType<V> type;
        @NotNull String name;
        @NotNull Set<String> aliases = new HashSet<>();

        @Value
        public class Usage {
            @Nullable V value;
        }
    }
}
