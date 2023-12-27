package org.comroid.api.data.seri;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.comroid.annotations.*;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.attr.Named;
import org.comroid.api.Polyfill;
import org.comroid.api.func.util.Streams;
import org.comroid.api.info.Constraint;
import org.comroid.api.func.util.Invocable;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.info.Log;
import org.comroid.api.java.Switch;
import org.comroid.api.text.Capitalization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.comroid.annotations.internal.Annotations.*;
import static org.comroid.api.Polyfill.uncheckedCast;
import static org.comroid.api.func.util.Streams.Multi.*;
import static org.comroid.api.text.Capitalization.*;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DataStructure<T> implements Named {
    // todo: fix WeakCache
    //private static final WeakCache<Key<?>, DataStructure<?>> $cache = new WeakCache<>(DataStructure::create);
    private static final Map<Key<?>, DataStructure<?>> $cache = new ConcurrentHashMap<>();
    public static final Map<Key<?>, DataStructure<?>> cache = Collections.unmodifiableMap($cache);

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
        class Helper {
            <R extends java.lang.reflect.Member & AnnotatedElement> Stream<R> streamRelevantMembers(Class<?> decl) {
                return Stream.of(decl).flatMap(Streams.multiply(
                                c -> Stream.of(c.getFields()),
                                c -> Stream.of(c.getMethods()),
                                c -> Stream.of(c.getConstructors())))
                        .map(Polyfill::uncheckedCast);
            }

            boolean filterModifiers(java.lang.reflect.Member member) {
                final var mod = member.getModifiers();
                return Map.<Class<?>, IntPredicate>of(
                                Field.class, bit -> !Modifier.isStatic(bit),
                                Method.class, bit -> ((Method) member).getReturnType().equals(target),
                                DataStructure.Constructor.class, bit -> true)
                        .entrySet().stream()
                        .allMatch(e -> !e.getKey().isInstance(member) || e.getValue().test(mod));
            }

            boolean filterIgnored(AnnotatedElement member) {
                return !Annotations.ignore(member, target);
            }

            <R extends java.lang.reflect.Member & AnnotatedElement> boolean filterPropertyMembers(R member) {
                if (member instanceof Field fld)
                    return true;
                else if (member instanceof Method mtd)
                    return (member.getName().startsWith("get") && member.getName().length() > 3);
                else return false;
            }

            <R extends java.lang.reflect.Member & AnnotatedElement, P> Stream<DataStructure<T>.Property<P>> convertProperties(R member) {
                String name = member.getName();
                ValueType<P> type = null;
                Invocable<P> getter = null;
                Invocable<?> setter = null;
                if (member instanceof Field fld) {
                    type = StandardValueType.forClass(fld.getType()).cast();
                    getter = Invocable.ofFieldGet(fld);
                    if (!Modifier.isFinal(member.getModifiers()))
                        setter = Invocable.ofFieldSet(fld);
                } else if (member instanceof Method mtd) {
                    name = lowerCamelCase.convert(name.substring(3));
                    type = StandardValueType.forClass(mtd.getReturnType()).cast();
                    getter = Invocable.ofMethodCall(mtd);
                    try {
                        var method = target.getMethod("set" + lowerCamelCase.convert(UpperCamelCase, name), type.getTargetClass());
                        setter = Invocable.ofMethodCall(method);
                    } catch (NoSuchMethodException ignored) {
                    }
                }
                if (type == null)
                    return Stream.empty();
                return Stream.of(struct.new Property<>(name, member, target, type, getter, setter))
                        .map(Polyfill::uncheckedCast);
            }

            <R extends java.lang.reflect.Member & AnnotatedElement> boolean filterConstructorMembers(R member) {
                if (member instanceof java.lang.reflect.Constructor<?> ctor)
                    return true;
                else if (member instanceof Method mtd)
                    return mtd.getReturnType().equals(target) && mtd.getParameterCount() == 0;
                else return false;
            }

            <R extends java.lang.reflect.Member & AnnotatedElement> Stream<DataStructure<T>.Constructor> convertConstructors(R member) {
                String name = member.getName();
                Invocable<T> func = null;
                if (member instanceof java.lang.reflect.Constructor<?> ctor)
                    func = Invocable.ofConstructor(uncheckedCast(ctor));
                else if (member instanceof Method mtd)
                    func = Invocable.ofMethodCall(mtd);
                if (func == null)
                    return Stream.empty();
                return Stream.of(struct.new Constructor(name, member, target, func));
            }
        }
        var helper = new Helper();
        var count = helper.streamRelevantMembers(target)
                .filter(helper::filterModifiers)
                .filter(helper::filterIgnored)
                .map(expand(Function.identity()))
                .flatMap(filter(helper::filterPropertyMembers, helper::filterConstructorMembers))
                .flatMap(flatMapA(helper::convertProperties))
                .flatMap(flatMapB(helper::convertConstructors))
                .peek(peek(
                        member -> struct.properties.put(member.name, uncheckedCast(member)),
                        member -> struct.constructors.add(uncheckedCast(member))))
                .flatMap(combine(Stream::of))
                .count();
        Log.at(Level.INFO, "Initialized %d members for %s".formatted(count, target.getCanonicalName()));
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
