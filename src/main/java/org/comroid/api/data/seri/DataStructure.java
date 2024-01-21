package org.comroid.api.data.seri;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.comroid.annotations.*;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.attr.Named;
import org.comroid.api.Polyfill;
import org.comroid.api.func.ValuePointer;
import org.comroid.api.func.util.Streams;
import org.comroid.api.info.Constraint;
import org.comroid.api.func.util.Invocable;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.info.Log;
import org.comroid.api.text.Capitalization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;
import java.util.logging.Level;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.comroid.annotations.internal.Annotations.*;
import static org.comroid.api.Polyfill.uncheckedCast;
import static org.comroid.api.java.ReflectionHelper.declaringClass;
import static org.comroid.api.java.ReflectionHelper.simpleClassName;
import static org.comroid.api.text.Capitalization.*;

@Value
@lombok.extern.java.Log
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DataStructure<T> implements Named {
    // todo: fix WeakCache
    //private static final WeakCache<Key<?>, DataStructure<?>> $cache = new WeakCache<>(DataStructure::create);
    private static final Map<Key<?>, DataStructure<?>> $cache = new ConcurrentHashMap<>();
    public static final Map<Key<?>, DataStructure<?>> cache = Collections.unmodifiableMap($cache);

    @NotNull Class<? super T> type;
    @NotNull
    @ToString.Exclude
    List<DataStructure<? super T>> parents = new ArrayList<>();
    @NotNull
    @ToString.Exclude
    List<Constructor> constructors = new ArrayList<>();
    @NotNull
    @ToString.Exclude
    Map<String, Property<?>> declaredProperties = new ConcurrentHashMap<>();

    public Set<DataStructure<? super T>.Property<?>> getProperties() {
        var set = new HashSet<DataStructure<? super T>.Property<?>>(declaredProperties.values());
        parents.stream().flatMap(dataStructure -> dataStructure.getProperties().stream())
                .filter(prop -> set.stream().map(Member::getName).noneMatch(prop.name::equals))
                .forEach(set::add);
        return set;
    }

    public List<DataStructure<? super T>.Property<?>> getOrderedProperties() {
        var list = new ArrayList<>(getProperties());
        list.sort(Property.COMPARATOR);
        return list;
    }

    public List<Map.Entry<Category.Adapter, List<DataStructure<? super T>.Property<?>>>> getCategorizedOrderedProperties() {
        Map<Category.Adapter, List<DataStructure<? super T>.Property<?>>> map = new ConcurrentHashMap<>();
        for (var prop : getProperties()) {
            var cat = prop.getCategory().orElse(Category.None);
            map.computeIfAbsent(cat, $ -> new ArrayList<>()).add(prop);
        }
        List<Map.Entry<Category.Adapter, List<DataStructure<? super T>.Property<?>>>> toSort = new ArrayList<>();
        for (Map.Entry<Category.Adapter, List<DataStructure<? super T>.Property<?>>> adapterListEntry : map.entrySet()) {
            var list = adapterListEntry.getValue();
            list.sort(Property.COMPARATOR);
            Map.Entry<Category.Adapter, List<DataStructure<? super T>.Property<?>>> apply
                    = new AbstractMap.SimpleImmutableEntry<>(adapterListEntry.getKey(), Collections.unmodifiableList(list));
            toSort.add(apply);
        }
        toSort.sort(Streams.comparatorAdapter(Map.Entry::getKey, Category.COMPARATOR));
        return Collections.unmodifiableList(toSort);
    }

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
        return Wrap.ofStream(getProperties().stream()
                        .filter(prop -> Stream.concat(Stream.of(prop.name), prop.aliases.stream())
                                .anyMatch(alias -> alias.equals(name))))
                .castRef();
    }

    public Set<Property<?>> update(Map<String, String> data, T target) {
        final var affected = new HashSet<Property<?>>();

        for (final var prop : declaredProperties.values()) {
            final var type = prop.type;
            if (!(type instanceof StandardValueType<?>)) {
                log.fine("Skipping auto-update for " + prop + " because its not a standard type (" + type + ")");
                continue;
            }

            final var name = prop.getName();
            if (!data.containsKey(name))
                continue;
            if (!prop.canSet()) {
                if (data.containsKey(name))
                    log.warning("Data had value for " + prop + "; but the property is not settable");
                continue;
            }

            var value = data.get(name);
            var parse = type.parse(value);
            prop.setFor(target, uncheckedCast(parse));
            affected.add(prop);
        }

        return Collections.unmodifiableSet(affected);
    }

    public static <T> DataStructure<T> of(@NotNull Class<? super T> target) {
        return of(target, Object.class);
    }

    @lombok.Builder
    public static <T> DataStructure<T> of(final @NotNull Class<? super T> target, final @NotNull Class<? super T> above) {
        final var key = new Key<>(target, above);
        if ($cache.containsKey(key))
            return uncheckedCast($cache.get(key));

        final var struct = new DataStructure<T>(target);

        var helper = new Object() {
            <R extends java.lang.reflect.Member & AnnotatedElement> Stream<R> streamRelevantMembers(Class<?> decl) {
                return Stream.of(decl).flatMap(Streams.multiply(
                                c -> Arrays.stream(c.getDeclaredFields()),
                                c -> Arrays.stream(c.getDeclaredMethods()),
                                c -> Arrays.stream(c.getConstructors())))
                        .map(Polyfill::uncheckedCast);
            }

            boolean filterDynamic(java.lang.reflect.Member member) {
                return !member.getName().matches("this\\$\\d+");
            }

            boolean filterSystem(java.lang.reflect.Member member) {
                var pkg = member.getDeclaringClass().getPackageName();
                return !pkg.startsWith("java");
            }

            boolean filterIgnored(AnnotatedElement member) {
                return Annotations.ignore(member, target).isEmpty();
            }

            boolean filterAbove(AnnotatedElement member) {
                if (member instanceof Class<?> cls) return !cls.isAssignableFrom(above);
                if (!(member instanceof AccessibleObject obj)) return true;
                return !declaringClass(obj).isAssignableFrom(above);
            }

            boolean filterPropertyModifiers(java.lang.reflect.Member member) {
                final var mod = member.getModifiers();
                return !Modifier.isStatic(mod) && (member instanceof Field || Modifier.isPublic(mod));
            }

            boolean filterConstructorModifiers(java.lang.reflect.Member member) {
                final var mod = member.getModifiers();
                return Map.<Class<?>, IntPredicate>of(
                                Method.class, bit -> ((Method) member).getReturnType().equals(target) && Modifier.isStatic(bit),
                                DataStructure.Constructor.class, bit -> true)
                        .entrySet().stream()
                        .anyMatch(e -> !e.getKey().isInstance(member) || e.getValue().test(mod));
            }

            <R extends java.lang.reflect.Member & AnnotatedElement> boolean filterPropertyMembers(R member) {
                if (member instanceof Field fld)
                    return checkAccess(fld) && !struct.getDeclaredProperties().containsKey(member.getName());
                else if (member instanceof Method mtd)
                    return checkAccess(mtd)
                            && (member.getName().startsWith("get") && member.getName().length() > 3)
                            && mtd.getParameterCount() == 0
                            && !struct.getDeclaredProperties().containsKey(Capitalization.Current.getProperties()
                            .convert(member.getName().substring(3)));
                else return false;
            }

            <R extends java.lang.reflect.Member & AnnotatedElement, P> DataStructure<T>.Property<P> convertProperty(R member) {
                final var parts = new ArrayList<AnnotatedElement>();
                final var name = new String[]{member.getName()};
                P defaultValue = Annotations.defaultValue(member);
                ValueType<P> type;
                Invocable<P> getter;
                Invocable<?> setter = null;

                parts.add(member);
                if (member instanceof Field fld) {
                    type = ValueType.of(fld.getType());
                    getter = Invocable.ofFieldGet(fld);
                    if (!Modifier.isFinal(member.getModifiers()))
                        setter = Invocable.ofFieldSet(fld);

                    Arrays.stream(target.getMethods())
                            .filter(mtd -> mtd.getName().toLowerCase().endsWith(name[0].toLowerCase()))
                            .filter(this::filterSystem)
                            .filter(this::filterIgnored)
                            .filter(this::filterAbove)
                            .filter(this::filterPropertyModifiers)
                            .forEach(parts::add);
                } else if (member instanceof Method mtd) {
                    name[0] = lowerCamelCase.convert(name[0].substring(3));
                    type = ValueType.of(mtd.getReturnType());
                    getter = Invocable.ofMethodCall(mtd);

                    setter = Wrap.ofOptional(Arrays.stream(target.getMethods())
                                    .filter(this::filterSystem)
                                    .filter(this::filterIgnored)
                                    .filter(this::filterAbove)
                                    .filter(this::filterPropertyModifiers)
                                    .filter(candidate -> {
                                        var setterName = candidate.getName();
                                        return setterName.startsWith("set")
                                                && setterName.length() > 3
                                                && setterName.equals("set" + UpperCamelCase.convert(name[0]))
                                                && checkAccess(candidate)
                                                && candidate.getParameterCount() == 1
                                                && ValueType.of(candidate.getParameterTypes()[0]).equals(type);
                                    })
                                    .findAny())
                            .peek(parts::add)
                            .ifPresentMap(Invocable::ofMethodCall);

                    Arrays.stream(target.getDeclaredFields())
                            .filter(fld -> fld.getName().equals(name[0]))
                            .forEach(parts::add);
                } else throw new AssertionError("Could not initialize property adapter for " + member);

                var partsArray = parts.toArray(AnnotatedElement[]::new);
                DataStructure<T>.Property<P> prop = uncheckedCast(struct.new Property<>(name[0], member, target, type, defaultValue, setter == null || parts.stream().anyMatch(Annotations::readonly), getter, setter));
                setAnnotations(prop, partsArray);
                setMetadata(prop, partsArray);
                return prop;
            }

            <R extends java.lang.reflect.Member & AnnotatedElement> boolean filterConstructorMembers(R member) {
                if (member instanceof java.lang.reflect.Constructor<?> ctor)
                    return checkAccess(ctor);
                else if (member instanceof Method mtd)
                    return checkAccess(mtd) && mtd.getReturnType().equals(target);
                else return false;
            }

            <R extends java.lang.reflect.Member & AnnotatedElement> DataStructure<T>.Constructor convertConstructor(R member) {
                String name = member.getName();
                Invocable<T> func = null;
                Parameter[] param = new Parameter[0];
                if (member instanceof java.lang.reflect.Constructor<?> ctor) {
                    func = Invocable.ofConstructor(uncheckedCast(ctor));
                    param = ctor.getParameters();
                } else if (member instanceof Method mtd) {
                    func = Invocable.ofMethodCall(mtd);
                    param = mtd.getParameters();
                }
                if (func == null)
                    throw new AssertionError("Could not initialize construction adapter for " + member);
                DataStructure<T>.Constructor ctor = struct.new Constructor(name, member, target, List.of(param), func);
                setAnnotations(ctor, member);
                setMetadata(ctor, member);
                return ctor;
            }

            private void setAnnotations(Member member, AnnotatedElement... sources) {
                // TODO: improve
                Arrays.stream(sources)
                        .flatMap(source -> Arrays.stream(source.getAnnotations())
                                .map(anno -> new Result<>(anno, source, source, declaringClass(source))))
                        .forEach(member.annotations::add);
                //member.annotations.addAll(findAnnotations(Annotation.class, source).toList());
            }

            private void setMetadata(Member member, AnnotatedElement... sources) {
                // aliases
                Arrays.stream(sources)
                        .flatMap(it -> Annotations.aliases(it).stream())
                        .forEach(member.aliases::add);

                // description
                Arrays.stream(sources)
                        .flatMap(Annotations::description)
                        .map(Result::getAnnotation)
                        .map(Annotations::toString)
                        .forEachOrdered(member.description::add);

                // categories
                Arrays.stream(sources)
                        .flatMap(it -> Annotations.category(it).stream())
                        .forEachOrdered(member.category::complete);
            }

            boolean checkAccess(AnnotatedElement member) {
                return member instanceof AccessibleObject obj && obj.trySetAccessible();
            }
        };

        var init = helper.streamRelevantMembers(target)
                .filter(helper::filterDynamic)
                .filter(helper::filterSystem)
                .filter(helper::filterIgnored)
                .filter(helper::filterAbove)
                .flatMap(s -> Stream.concat(
                        Stream.of(s)
                                .filter(helper::filterPropertyModifiers)
                                .filter(helper::filterPropertyMembers)
                                .map(helper::convertProperty)
                                .peek(member -> Stream.concat(Stream.of(member.getName()), member.aliases.stream())
                                        .map(Current.getProperties()::convert)
                                        .forEach(name -> struct.declaredProperties.put(name, uncheckedCast(member)))),
                        Stream.of(s)
                                .filter(helper::filterConstructorModifiers)
                                .filter(helper::filterConstructorMembers)
                                .map(helper::convertConstructor)
                                .peek(ctor -> struct.constructors.add(uncheckedCast(ctor)))))
                .toList();

        // init parents
        $cache.put(key, struct);
        Stream.of(target.getSuperclass())
                .collect(Streams.append(target.getInterfaces()))
                .filter(Objects::nonNull)
                .filter(helper::filterAbove)
                .map(DataStructure::of)
                .map(Polyfill::<DataStructure<? super T>>uncheckedCast)
                .forEach(struct.parents::add);

        Log.at(Level.FINE, "Initialized %d members for %s".formatted(init.size(), target.getCanonicalName()));
        Log.at(Level.FINER, "Initialized: "+init.stream().map(Objects::toString).collect(Collectors.joining("\n\t- ","\n\t- ","")));
        return struct;
    }

    @FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
    public static abstract class Member implements Named, AnnotatedElement, java.lang.reflect.Member, ValuePointer<Object> {
        @NotNull
        @Getter
        String name;
        @Getter @NotNull Set<String> aliases = new HashSet<>();
        @Getter @NotNull List<String> description = new ArrayList<>();
        @Getter @NotNull Wrap.Future<Category.Adapter> category = new Wrap.Future<>();
        @NotNull
        @ToString.Exclude
        @Getter(onMethod = @__(@JsonIgnore))
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
            return Title_Case.convert(getName());
        }

        @Ignore
        @Override
        @JsonIgnore
        public Annotation[] getAnnotations() {
            return streamAnnotations(Annotation.class).toArray(Annotation[]::new);
        }

        @Ignore
        @Override
        @JsonIgnore
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

        public abstract String getCanonicalName();

        @Override
        public String toString() {
            return "%s (%s) %s.%s".formatted(
                    getClass().getSimpleName(),
                    getHeldType().getTargetClass().getSimpleName(),
                    declaringClass.getSimpleName(),
                    name);
        }
    }

    @Value
    public class Constructor extends Member {
        @NotNull
        @ToString.Exclude
        @Getter(onMethod = @__(@JsonIgnore))
        List<Parameter> args;
        @NotNull
        @ToString.Exclude
        @Getter(onMethod = @__(@JsonIgnore))
        Invocable<T> ctor;

        private Constructor(@NotNull String name,
                            @NotNull AnnotatedElement context,
                            @NotNull Class<?> declaringClass,
                            @NotNull List<Parameter> args,
                            @NotNull Invocable<T> ctor) {
            super(name, context, declaringClass);
            this.args = args;
            this.ctor = ctor;
        }

        @Override
        public int getModifiers() {
            return super.getModifiers() | Modifier.STATIC;
        }

        @Override
        public ValueType<?> getHeldType() {
            return ValueType.of(declaringClass);
        }

        @Override
        public String getCanonicalName() {
            return declaringClass.getCanonicalName() + ".ctor";
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    @Value
    public class Property<V> extends Member {
        public static final Comparator<? super DataStructure<?>.Property<?>> COMPARATOR = Comparator
                .<DataStructure<?>.Property<?>>comparingInt(prop -> prop.getCategory().stream()
                        .mapToInt(Category::order)
                        .findFirst()
                        .orElse(0))
                .thenComparing(Order.COMPARATOR);
        @NotNull ValueType<V> type;
        @Nullable V defaultValue;
        boolean readonly;
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
                        @Nullable V defaultValue,
                        boolean readonly,
                        @Nullable Invocable<V> getter,
                        @Nullable Invocable<?> setter) {
            super(name, context, declaringClass);
            this.type = type;
            this.defaultValue = defaultValue;
            this.readonly = readonly;
            this.getter = getter;
            this.setter = setter;
        }

        public @Nullable V getFrom(T target) {
            Constraint.notNull(getter, "getter").run();
            Constraint.notNull(target, "target").run();
            return getter.invokeSilent(target);
        }

        public @Nullable V setFor(T target, V value) {
            Constraint.decide(!readonly, "not readonly");
            Constraint.decide(canSet(), "canSet");
            Constraint.notNull(value, "value");
            var prev = getFrom(target);
            setter.invokeSilent(target, value);
            return prev;
        }

        @Override
        public int getModifiers() {
            return super.getModifiers() | (setter == null ? Modifier.FINAL : 0);
        }

        @Override
        public ValueType<?> getHeldType() {
            return type;
        }

        @Override
        public String getCanonicalName() {
            return declaringClass.getCanonicalName() + '.' + name;
        }

        @Override
        public String toString() {
            return super.toString() + " (" + (getter!=null?"get":"")+(setter!=null?" set":"")+')';
        }

        public boolean canSet() {
            return setter != null;
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
