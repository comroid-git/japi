package org.comroid.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.comroid.annotations.*;
import org.comroid.util.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.comroid.api.Polyfill.uncheckedCast;
import static org.comroid.util.Streams.yield;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DataStructure<T> implements Named {
    // todo: fix WeakCache
    //private static final WeakCache<Key<?>, DataStructure<?>> $cache = new WeakCache<>(DataStructure::create);
    private static final Map<Key<?>, DataStructure<?>> $cache = new ConcurrentHashMap<>();
    public static final Map<Key<?>, DataStructure<?>> cache = Collections.unmodifiableMap($cache);
    public static final Class<?>[] SystemFilters = new Class<?>[]{Object.class, Class.class};

    @NotNull Class<? super T> type;
    @NotNull
    @ToString.Exclude
    List<Constructor> constructors = new ArrayList<>();
    @NotNull
    @ToString.Exclude
    Map<String, Property<?>> properties = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return type.getCanonicalName();
    }

    public <V> SupplierX<Property<V>> getProperty(AnnotatedElement member) throws ClassCastException {
        return getProperty((java.lang.reflect.Member) member);
    }

    public <V> SupplierX<Property<V>> getProperty(java.lang.reflect.Member member) {
        return getProperty(member.getName());
    }

    public <V> SupplierX<Property<V>> getProperty(String name) {
        return SupplierX.of(properties.getOrDefault(name, null)).castRef();
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
                        .filter(mtd -> mtd.getName().equalsIgnoreCase("set" + name))
                        .findAny()
                        .map(Invocable::ofMethodCall)
                        .orElse(null))
                .apply(it);

        Objects.requireNonNull(getter, "Getter could not be initialized");
        return new Property<>(name, it, uncheckedCast(vt), getter, setter);
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
                                .filter(mtd -> Stream.of(Instance.class, Convert.class)
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

                    var ctor = struct.new Constructor(it.getName(), it, func);
                    for (var parameter : it.getParameters())
                        ctor.args.put(parameter.getName(), struct.createProperty(parameter.getType(), parameter.getName(), parameter));
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

    @FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
    public static abstract class Member implements Named, AnnotatedElement, java.lang.reflect.Member {
        @NotNull
        @Getter
        String name;
        @NotNull
        @Getter
        Set<String> aliases = new HashSet<>();
        @NotNull
        @ToString.Exclude
        AnnotatedElement context;
        @NotNull
        @ToString.Exclude
        Set<Annotations.Result<?>> annotations = new HashSet<>();
        @Getter
        @NotNull Class<?> declaringClass;
        @Getter
        int modifiers = Modifier.PUBLIC;
        @Getter
        boolean synthetic = false;

        private Member(@NotNull String name, @NotNull AnnotatedElement context) {
            this.name = name;
            this.declaringClass = ReflectionHelper.declaringClass(context);
            this.context = context;

            Annotations.findAnnotations(Alias.class, context)
                    .map(Annotations.Result::getAnnotation)
                    .flatMap(alias -> Arrays.stream(alias.value()))
                    .forEach(aliases::add);
            Annotations.findAnnotations(Annotation.class, context).forEach(annotations::add);
        }

        @Override
        public String getAlternateName() {
            return aliases.stream().findAny().orElseGet(Named.super::getAlternateName);
        }

        @Override
        public Annotation[] getAnnotations() {
            return streamAnnotations(Annotation.class).toArray(Annotation[]::new);
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return streamAnnotations(Annotation.class)
                    .filter(result -> result.getContext().equals(context))
                    .toArray(Annotation[]::new);
        }

        public <A extends Annotation> Stream<Annotations.Result<A>> streamAnnotations(Class<A> annotationClass) {
            return annotations.stream()
                    .map(Polyfill::<Annotations.Result<A>>uncheckedCast)
                    .filter(result -> annotationClass.isAssignableFrom(result.annotationType()));
        }

        @Override
        @SuppressWarnings({"NullableProblems", "DataFlowIssue"}) // killing away weird intellij complaints
        public <T extends Annotation> @NotNull @Nullable T getAnnotation(final @NotNull Class<T> annotationClass) {
            return streamAnnotations(annotationClass)
                    .map(Annotations.Result::getAnnotation)
                    .findAny()
                    .orElse(null);
        }
    }

    @Value
    public class Constructor extends Member {
        @NotNull
        @ToString.Exclude
        Map<String, Property<?>> args = new ConcurrentHashMap<>();
        @NotNull
        @ToString.Exclude
        @Getter(onMethod = @__(@JsonIgnore))
        Invocable<T> ctor;

        private Constructor(@NotNull String name,
                            @NotNull AnnotatedElement context,
                            @NotNull Invocable<T> ctor) {
            super(name, context);
            this.ctor = ctor;
        }

        @Override
        public int getModifiers() {
            return super.getModifiers() | Modifier.STATIC;
        }
    }

    @Value
    public class Property<V> extends Member {
        @NotNull ValueType<V> type;
        @NotNull
        @ToString.Exclude
        @Getter(onMethod = @__(@JsonIgnore))
        Invocable<V> getter;
        @Nullable
        @ToString.Exclude
        @Getter(onMethod = @__(@JsonIgnore))
        Invocable<?> setter;

        public Property(@NotNull String name,
                        @NotNull AnnotatedElement context,
                        @NotNull ValueType<V> type,
                        @NotNull Invocable<V> getter,
                        @Nullable Invocable<?> setter) {
            super(name, context);
            this.type = type;
            this.getter = getter;
            this.setter = setter;
        }

        @SneakyThrows
        public @Nullable V getFrom(T target) {
            Constraint.notNull(getter, "getter");
            return getter.invoke(target);
        }

        @Override
        public int getModifiers() {
            return super.getModifiers() | (setter == null ? Modifier.FINAL : 0);
        }

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public abstract class Mod {
            public DataStructure<T> getStructure() {
                return DataStructure.this;
            }

            public Property<V> getProperty() {
                return Property.this; // for serialization
            }
        }

        @Value
        public class Usage extends Mod {
            @Nullable V value;
        }

        @Value
        public class Bound extends Mod implements SupplierX<V> {
            @NotNull T target;

            @Override
            public @Nullable V get() {
                return getFrom(target);
            }
        }
    }

    @Value
    public static class Key<T> {
        @NotNull Class<? super T> type;
        @NotNull Class<? super T> above;
    }
}
