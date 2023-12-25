package org.comroid.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.comroid.annotations.*;
import org.comroid.util.BoundValueType;
import org.comroid.util.Constraint;
import org.comroid.util.StandardValueType;
import org.comroid.util.Switch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.comroid.api.Polyfill.uncheckedCast;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DataStructure<T> implements Named {
    // todo: fix WeakCache
    //private static final WeakCache<Key<?>, DataStructure<?>> $cache = new WeakCache<>(DataStructure::create);
    private static final Map<Key<?>, DataStructure<?>> $cache = new ConcurrentHashMap<>();
    public static final Map<Key<?>, DataStructure<?>> cache = Collections.unmodifiableMap($cache);
    public static final Class<?>[] SystemFilters = new Class<?>[]{Object.class,Class.class};

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
        var prop = new Property<>(name, uncheckedCast(vt), getter, setter);
        var aliases = it.getAnnotation(Alias.class);
        if (aliases != null) prop.aliases.addAll(Arrays.asList(aliases.value()));
        Annotations.findAnnotations(Annotation.class, it).forEach(prop.annotations::add);
        return prop;
    }

    public static <T> DataStructure<T> of(@NotNull Class<? super T> target) {
        return of(target, Object.class);
    }

    @lombok.Builder
    public static <T> DataStructure<T> of(@NotNull Class<? super T> target, @NotNull Class<? super T> above) {
        //return uncheckedCast($cache.touch(new Key<T>(target, above)));
        return uncheckedCast($cache.computeIfAbsent(new Key<T>(target, above), DataStructure::create));
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
                        Arrays.stream(target.getConstructors()))
                .filter(it -> !Annotations.ignore(it, DataStructure.class))
                .filter(it -> !key.above.equals(it.getDeclaringClass()) && key.above.isAssignableFrom(it.getDeclaringClass()))
                .filter(it -> Arrays.stream(SystemFilters).noneMatch(type -> it.getDeclaringClass().isAssignableFrom(type)))
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
                    Annotations.findAnnotations(Annotation.class, it).forEach(ctor.annotations::add);
                    return ctor;
                }).forEach(struct.constructors::add);

        // properties
        Stream.concat(Arrays.stream(target.getFields()),
                        Arrays.stream(target.getMethods())
                                .filter(mtd -> mtd.getParameterCount() == 0)
                                .filter(mtd -> mtd.getName().startsWith("get") && mtd.getName().length() > 3))
                .filter(it -> !Modifier.isStatic(it.getModifiers()))
                .filter(it -> Modifier.isPublic(it.getModifiers()))
                .filter(it -> !Annotations.ignore(it, DataStructure.class))
                .filter(it -> !key.above.equals(it.getDeclaringClass()) && key.above.isAssignableFrom(it.getDeclaringClass()))
                .filter(it -> Arrays.stream(SystemFilters).noneMatch(type -> it.getDeclaringClass().isAssignableFrom(type)))
                .forEach(it -> {
                    var type = new Switch<java.lang.reflect.Member, Class<?>>()
                            .option(Field.class::isInstance, () -> ((Field) it).getType())
                            .option(Method.class::isInstance, () -> ((Method) it).getReturnType())
                            .apply(it);
                    var name = it.getName();
                    if (it instanceof Method)
                        name = Character.toLowerCase(name.charAt(3)) + name.substring("getX".length());
                    var prop = struct.createProperty(type, name, it);
                    Stream.concat(Stream.of(prop.name), Annotations.aliases(it).stream())
                            .forEach(k -> struct.properties.put(k, prop));
                });

        return struct;
    }

    @Data
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
    public static abstract class Member implements Named {
        @NotNull String name;
        @NotNull Set<String> aliases = new HashSet<>();
        @NotNull @ToString.Exclude Set<Annotations.Result<?>> annotations = new HashSet<>();

        @Override
        public String getAlternateName() {
            return aliases.stream().findAny().orElseGet(Named.super::getAlternateName);
        }
    }

    @Value
    public class Constructor extends Member {
        @NotNull @ToString.Exclude Map<String, Property<?>> args = new ConcurrentHashMap<>();
        @NotNull @ToString.Exclude @Getter(onMethod = @__(@JsonIgnore)) Invocable<T> ctor;

        private Constructor(@NotNull String name, @NotNull Invocable<T> ctor) {
            super(name);
            this.ctor = ctor;
        }
    }

    @Value
    public class Property<V> extends Member {
        @NotNull ValueType<V> type;
        @Nullable @ToString.Exclude @Getter(onMethod = @__(@JsonIgnore)) Invocable<V> getter;
        @Nullable @ToString.Exclude @Getter(onMethod = @__(@JsonIgnore)) Invocable<?> setter;

        private Property(@NotNull String name, @NotNull ValueType<V> type, @Nullable Invocable<V> getter, @Nullable Invocable<?> setter) {
            super(name);
            this.type = type;
            this.getter = getter;
            this.setter = setter;
        }

        @SneakyThrows
        @SuppressWarnings("DataFlowIssue")
        public @Nullable V getFrom(T target) {
            Constraint.notNull(getter, "getter");
            return getter.invoke(target);
        }

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
