package org.comroid.api.data.seri;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.comroid.annotations.*;
import org.comroid.api.attr.Named;
import org.comroid.api.Polyfill;
import org.comroid.api.info.Constraint;
import org.comroid.api.func.util.Invocable;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.java.Switch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static org.comroid.annotations.Annotations.*;
import static org.comroid.api.Polyfill.uncheckedCast;
import static org.comroid.api.func.util.Streams.Multi.*;
import static org.comroid.api.java.ReflectionHelper.declaringClass;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DataStructure<T> implements Named {
    // todo: fix WeakCache
    //private static final WeakCache<Key<?>, DataStructure<?>> $cache = new WeakCache<>(DataStructure::create);
    private static final Map<Key<?>, DataStructure<?>> $cache = new ConcurrentHashMap<>();
    public static final Map<Key<?>, DataStructure<?>> cache = Collections.unmodifiableMap($cache);
    public static final Class<?>[] SystemFilters = new Class<?>[]{Object.class, Class.class, Annotation.class};

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

    public <V> Wrap<Property<V>> getProperty(AnnotatedElement member) throws ClassCastException {
        return getProperty((java.lang.reflect.Member) member);
    }

    public <V> Wrap<Property<V>> getProperty(java.lang.reflect.Member member) {
        return getProperty(member.getName());
    }

    public <V> Wrap<Property<V>> getProperty(String name) {
        return Wrap.of(properties.getOrDefault(name, null)).castRef();
    }

    private <V> Property<V> createProperty(Class<V> type, String name, AnnotatedElement it, Class<?> decl) {
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

        return new Property<>(name, it, decl, uncheckedCast(vt), getter, setter);
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
                .filter(it -> !ignore(it, DataStructure.class))
                .map(explode(java.lang.reflect.Member::getDeclaringClass))
                .flatMap(filterB(decl -> !key.above.equals(decl) && key.above.isAssignableFrom(decl)))
                .flatMap(filterB(decl -> {
                    var packageName = decl.getPackageName();
                    return !packageName.startsWith("java") || "java.lang".equals(packageName);
                }))
                .map(Map.Entry::getKey)
                .filter(it -> Modifier.isPublic(it.getModifiers()))
                .map(it -> {
                    Invocable<T> func;
                    if (it instanceof java.lang.reflect.Constructor<?>)
                        func = Invocable.ofConstructor(uncheckedCast(it));
                    else if (it instanceof Method)
                        func = Invocable.ofMethodCall((Method) it);
                    else throw new AssertionError();

                    var declaringClass = declaringClass(it);
                    var ctor = struct.new Constructor(it.getName(), it, declaringClass, func);
                    for (var parameter : it.getParameters())
                        ctor.args.put(parameter.getName(), struct.createProperty(parameter.getType(), parameter.getName(), parameter, declaringClass));
                    return ctor;
                }).forEach(struct.constructors::add);

        // properties
        Stream.concat(Arrays.stream(target.getFields()),
                        Arrays.stream(target.getMethods())
                                .filter(mtd -> mtd.getParameterCount() == 0)
                                .filter(mtd -> mtd.getName().startsWith("get") && mtd.getName().length() > 3))
                .map(explode(java.lang.reflect.Member::getModifiers))
                .flatMap(filter(it -> !ignore(it, DataStructure.class), mod -> !Modifier.isStatic(mod) && Modifier.isPublic(mod)))
                .map(crossA2B(java.lang.reflect.Member::getDeclaringClass))
                .flatMap(filterB(not(Arrays.asList(SystemFilters)::contains)))
                .flatMap(filterB(decl -> {
                    var packageName = decl.getPackageName();
                    return (!packageName.startsWith("java") || "java.lang".equals(packageName))
                            && key.above.isAssignableFrom(decl);
                }))
                .forEach(forEach((it, decl) -> {
                    var type = new Switch<java.lang.reflect.Member, Class<?>>()
                            .option(Field.class::isInstance, () -> ((Field) it).getType())
                            .option(Method.class::isInstance, () -> ((Method) it).getReturnType())
                            .apply(it);
                    var name = it.getName();
                    if (it instanceof Method)
                        name = Character.toLowerCase(name.charAt(3)) + name.substring("getX".length());
                    var prop = struct.createProperty(type, name, it, decl);
                    Stream.concat(Stream.of(prop.name), aliases(it).stream())
                            .forEach(k -> struct.properties.put(k, prop));
                }));

        return struct;
    }

    @FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
    public static abstract class Member implements Named, AnnotatedElement, java.lang.reflect.Member {
        @NotNull
        @Getter
        String name;
        @Getter
        @NotNull
        Set<String> aliases = new HashSet<>();
        @Getter
        @NotNull
        @ToString.Exclude
        AnnotatedElement context;
        @NotNull
        @ToString.Exclude
        Set<Result<?>> annotations = new HashSet<>();
        @Getter
        @NotNull Class<?> declaringClass;
        @Getter
        int modifiers = Modifier.PUBLIC;
        @Getter
        boolean synthetic = false;

        private Member(@NotNull String name, @NotNull AnnotatedElement context, @NotNull Class<?> declaringClass) {
            this.name = name;
            this.context = context;
            this.declaringClass = declaringClass;

            findAnnotations(Alias.class, this)
                    .map(Result::getAnnotation)
                    .flatMap(alias -> Arrays.stream(alias.value()))
                    .forEach(aliases::add);
            findAnnotations(Annotation.class, this).forEach(annotations::add);
        }

        @Override
        public String getAlternateName() {
            return aliases.stream().findAny().orElseGet(Named.super::getAlternateName);
        }

        @Ignore
        @Override
        public Annotation[] getAnnotations() {
            return streamAnnotations(Annotation.class).toArray(Annotation[]::new);
        }

        @Ignore
        @Override
        public Annotation[] getDeclaredAnnotations() {
            return streamAnnotations(Annotation.class)
                    .filter(result -> result.getContext().equals(context))
                    .toArray(Annotation[]::new);
        }

        public Stream<Result<Annotation>> streamAnnotations() {
            return streamAnnotations(Annotation.class);
        }

        public <A extends Annotation> Stream<Result<A>> streamAnnotations(Class<A> annotationClass) {
            return annotations.stream()
                    .map(Polyfill::<Result<A>>uncheckedCast)
                    .filter(result -> annotationClass.isAssignableFrom(result.annotationType()));
        }

        @Override
        @SuppressWarnings({"NullableProblems", "DataFlowIssue"}) // killing away weird intellij complaints
        public <T extends Annotation> @NotNull @Nullable T getAnnotation(final @NotNull Class<T> annotationClass) {
            return streamAnnotations(annotationClass)
                    .map(Result::getAnnotation)
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
                            @NotNull Class<?> declaringClass,
                            @NotNull Invocable<T> ctor) {
            super(name, context, declaringClass);
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
        @Nullable
        @ToString.Exclude
        @Getter(onMethod = @__(@JsonIgnore))
        Invocable<V> getter;
        @Nullable
        @ToString.Exclude
        @Getter(onMethod = @__(@JsonIgnore))
        Invocable<?> setter;

        public Property(@NotNull String name,
                        @NotNull AnnotatedElement context,
                        @NotNull Class<?> declaringClass,
                        @NotNull ValueType<V> type,
                        @Nullable Invocable<V> getter,
                        @Nullable Invocable<?> setter) {
            super(name, context, declaringClass);
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
        public class Bound extends Mod implements Wrap<V> {
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
