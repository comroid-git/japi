package org.comroid.api.func.ext;

import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.experimental.NonFinal;
import org.comroid.api.Polyfill;
import org.comroid.api.func.Junction;
import org.comroid.api.func.util.Streams;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.comroid.api.func.exc.ThrowingFunction.*;
import static org.comroid.api.text.NameConverter.*;

public interface Accessor<Obj, Prop> extends Function<Obj, Prop>, BiConsumer<Obj, Prop> {
    @SneakyThrows
    static <Obj, Prop> Accessor<Obj, Prop> ofProperty(@NotNull Class<Obj> type, @NotNull String propertyName) {
        return Stream.concat(Stream.of(type)
                                .flatMap(fallback(cls -> findMember(cls, Class::getDeclaredFields, propertyName)))
                                .filter(Objects::nonNull)
                                .<Accessor<Obj, Prop>>map(Field::new),
                        Streams.merge(Stream.of(type).flatMap(fallback(cls -> findMember(cls, Class::getDeclaredMethods, GETTER.decorate(propertyName)))),
                                        Stream.of(type).flatMap(fallback(cls -> findMember(cls, Class::getDeclaredMethods, SETTER.decorate(propertyName)))))
                                .<Accessor<Obj, Prop>>map(accessors -> new GetSetMethods<>(accessors.a, accessors.b)))
                .sorted(Comparator.comparingInt(Accessor::getUsagePriority))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No such property '%s' in type %s".formatted(propertyName, type.getCanonicalName())));
    }

    @Override
    Prop apply(Obj obj);

    @Override
    void accept(Obj obj, Prop value);

    default Prop get(Obj obj) {
        return apply(obj);
    }

    default void set(Obj obj, Prop value) {
        accept(obj, value);
    }

    default Linked<Obj, Prop> link(Obj obj) {
        return new Linked<>(this, obj);
    }

    default <Out> Accessor<Obj, Out> proxy(Function<Prop, Out> serializer, Function<Out, Prop> deserializer) {
        return proxy(Junction.of(serializer, deserializer));
    }

    default <Out> Accessor<Obj, Out> proxy(final Junction<Prop, Out> converter) {
        return new Default<>(obj -> converter.forward(apply(obj)),
                (obj, out) -> accept(obj, converter.backward(out)));
    }

    interface Delegated<Obj, Prop> extends Accessor<Obj, Prop> {
        Accessor<Obj, Prop> getDelegateAccessor();

        @Override
        default Prop apply(Obj obj) {
            return getDelegateAccessor().apply(obj);
        }

        @Override
        default void accept(Obj obj, Prop value) {
            getDelegateAccessor().accept(obj, value);
        }
    }

    private static <T extends Member> Stream<T> findMember(Class<?> $cls, Function<Class<?>, T[]> extract, String name) {
        return Stream.ofNullable($cls)
                .flatMap(type -> Stream.concat(Arrays.stream(extract.apply(type)).filter(it -> it.getName().equals(name)),
                        Stream.concat(Stream.of(type.getSuperclass()), Arrays.stream(type.getInterfaces())).flatMap(cls -> findMember(cls, extract, name))));
    }

    private static int getUsagePriority(Accessor<?, ?> accessor) {
        return switch (accessor) {
            case Field<?, ?> field -> getUsagePriority(field.field);
            case Accessor.GetSetMethods<?, ?> methods -> (getUsagePriority(methods.getter) + getUsagePriority(methods.setter)) / 2;
            case Default<?, ?> $ -> 25;
            default -> 0;
        };
    }

    private static <T extends AccessibleObject & Member> int getUsagePriority(T member) {
        return Map.<Predicate<T>, @NotNull Integer>of(
                        it -> Modifier.isPrivate(it.getModifiers()), 1,
                        it -> Modifier.isProtected(it.getModifiers()), 2,
                        it -> Modifier.isPublic(it.getModifiers()), 8,
                        AccessibleObject::trySetAccessible, 12)
                .entrySet().stream()
                .filter(e -> e.getKey().test(member))
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    @Value
    @NonFinal
    class Default<Obj, Prop> implements Accessor<Obj, Prop> {
        @Delegate Function<Obj, Prop>   getter;
        @Delegate BiConsumer<Obj, Prop> setter;
    }

    @Value
    @NonFinal
    class Field<Obj, Prop> implements Accessor<Obj, Prop> {
        java.lang.reflect.Field field;

        @Override
        @SneakyThrows
        public Prop apply(Obj obj) {
            if (!field.canAccess(obj) && !field.trySetAccessible()) throw new IllegalStateException("Field is inaccessible");
            return Polyfill.uncheckedCast(field.get(obj));
        }

        @Override
        @SneakyThrows
        public void accept(Obj obj, Prop value) {
            if (!field.canAccess(obj) && !field.trySetAccessible()) throw new IllegalStateException("Field is inaccessible");
            field.set(obj, value);
        }
    }

    @Value
    @NonFinal
    class GetSetMethods<Obj, Prop> implements Accessor<Obj, Prop> {
        Method getter;
        Method setter;

        @Override
        @SneakyThrows
        public Prop apply(Obj obj) {
            if (!getter.canAccess(obj) && !getter.trySetAccessible()) throw new IllegalStateException("Getter is inaccessible");
            return Polyfill.uncheckedCast(getter.invoke(obj));
        }

        @Override
        @SneakyThrows
        public void accept(Obj obj, Prop value) {
            if (!setter.canAccess(obj) && !setter.trySetAccessible()) throw new IllegalStateException("Setter is inaccessible");
            setter.invoke(obj, value);
        }
    }

    @Value
    class Linked<Obj, Prop> implements Ref<Prop> {
        Accessor<Obj, Prop> accessor;
        Obj                 delegate;

        @Override
        public Prop get() {
            return accessor.apply(delegate);
        }

        @Override
        public void accept(Prop prop) {
            accessor.accept(delegate, prop);
        }
    }
}
