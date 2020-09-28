package org.comroid.util;

import org.comroid.annotations.Instance;
import org.comroid.api.Polyfill;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.reflect.Modifier.*;

public final class ReflectionHelper {
    public static <T> T instance(Class<T> type, Object... args) throws RuntimeException, AssertionError {
        final Optional<T> optInstByField = instanceField(type);

        if (optInstByField.isPresent()) {
            return optInstByField.get();
        }

        final Class<?>[] types = types(args);

        return Arrays.stream(type.getConstructors())
                .filter(constr -> constr.getParameterCount() == types.length)
                .filter(constr -> {
                    final Class<?>[] params = constr.getParameterTypes();

                    for (int i = 0; i < types.length; i++)
                        if (!params[i].isAssignableFrom(types[i]))
                            return false;

                    return true;
                })
                .findAny()
                .map(constr -> instance(constr, args))
                .map(Polyfill::<T>uncheckedCast)
                .orElseThrow(() -> new NoSuchElementException("No suitable constructor found in class: " + type));
    }

    public static <T> Optional<T> instanceField(Class<T> type) {
        return fieldWithAnnotation(type, Instance.class).stream()
                .filter(field -> typeCompat(type, field.getType()))
                .filter(field -> isStatic(field.getModifiers()) && isFinal(field.getModifiers()) &&
                        isPublic(field.getModifiers()))
                .findAny()
                .map(field -> {
                    try {
                        return (T) field.get(null);
                    } catch (IllegalAccessException e) {
                        throw new AssertionError("Cannot access field", e);
                    }
                });
    }

    public static Class<?>[] types(Object... args) {
        final Class<?>[] yields = new Class[args.length];

        for (int i = 0; i < args.length; i++) {
            yields[i] = args[i].getClass();
        }

        return yields;
    }

    public static <T> Optional<Constructor<T>> findConstructor(Class<T> inClass, Class<?>[] types) {
        final Constructor<?>[] constructors = inClass.getDeclaredConstructors();

        if (constructors.length == 0) {
            return Optional.empty();
        }

        return Stream.of(constructors)
                .map(it -> (Constructor<T>) it)
                .max(Comparator.comparingLong(constr -> Stream.of(constr.getParameterTypes())
                        .filter(typ -> Stream.of(types)
                                .anyMatch(typ::isAssignableFrom))
                        .count()));
    }

    public static <T> T instance(Constructor<T> constructor, Object... args) throws RuntimeException, AssertionError {
        try {
            return constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error in Constructor", e);
        } catch (IllegalAccessException e) {
            throw new AssertionError(String.format("Could not access constructor %s", constructor), e);
        } catch (InstantiationException e) {
            throw new AssertionError(String.format("Class %s is abstract",
                    constructor.getDeclaringClass()
                            .getName()
            ), e);
        }
    }

    public static Set<Field> fieldWithAnnotation(
            Class<?> type, Class<? extends Annotation> instanceClass
    ) {
        Set<Field> yields = new HashSet<>();

        for (Field field : type.getFields()) {
            if (field.isAnnotationPresent(instanceClass)) {
                yields.add(field);
            }
        }

        return yields;
    }

    public static boolean typeCompat(Class<?> expected, Class<?> target) {
        if (expected.getName().contains("."))
            // type is not primitive
            return expected.equals(target) || expected.isAssignableFrom(target);

        // type is primitive
        switch (expected.getName()) {
            case "int":
                return typeCompat(Integer.class, target);
            case "double":
                return typeCompat(Double.class, target);
            case "long":
                return typeCompat(Long.class, target);
            case "char":
                return typeCompat(Character.class, target);
            case "boolean":
                return typeCompat(Boolean.class, target);
            case "short":
                return typeCompat(Short.class, target);
            case "float":
                return typeCompat(Float.class, target);
        }

        return false;
    }

    public static <T> Set<T> collectStaticFields(
            Class<? extends T> fieldType,
            Class<?> inClass,
            boolean forceAccess,
            @Nullable Class<? extends Annotation> withAnnotation
    ) {
        final Field[] fields = inClass.getFields();
        final HashSet<T> values = new HashSet<>(fields.length);

        for (Field field : fields) {
            if (forceAccess && !field.isAccessible()) {
                field.setAccessible(true);
            }

            if (isStatic(field.getModifiers())
                    && fieldType.isAssignableFrom(field.getType())
                    && (withAnnotation == null || field.isAnnotationPresent(withAnnotation))) {
                try {
                    values.add(fieldType.cast(field.get(null)));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("", e);
                }
            }
        }

        values.removeIf(Objects::isNull);

        return Collections.unmodifiableSet(values);
    }

    public static Object[] arrange(Object[] args, Class<?>[] typesOrdered) {
        final Object[] yields = new Object[typesOrdered.length];

        for (int i = 0; i < typesOrdered.length; i++) {
            int finalli = i;
            yields[i] = Stream.of(args)
                    .filter(Objects::nonNull)
                    .filter(it -> typeCompat(typesOrdered[finalli], it.getClass()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "No instance of " + typesOrdered[finalli].getName() + " found in array"));
        }

        return yields;
    }

    public static <T> Class<? super T> canonicalClass(Class<T> of) {
        if (Object.class.equals(of) || Void.class.equals(of)) {
            return Object.class;
        }
        if (Modifier.isInterface(of.getModifiers()) || of.isPrimitive()) {
            return of;
        }
        if (of.isAnonymousClass()) {
            return canonicalClass(of.getSuperclass());
        }

        return of;
    }

    public static <A extends Annotation> Optional<A> findAnnotation(Class<A> annotation, Class<?> inClass, ElementType target) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (target) {
            case TYPE:
                return StreamSupport.stream(recursiveClassGenerator(inClass), false)
                        .filter(type -> type.isAnnotationPresent(annotation))
                        .findFirst()
                        .map(type -> type.getAnnotation(annotation));
            default:
                throw new UnsupportedOperationException("Please contact the developer");
        }
    }

    private static Spliterator<Class<?>> recursiveClassGenerator(Class<?> from) {
        return Spliterators.spliteratorUnknownSize(new Iterator<Class<?>>() {
            private final Queue<Class<?>> queue = new LinkedBlockingQueue<>();

            {
                queue.add(from);
            }

            @Override
            public boolean hasNext() {
                return !queue.isEmpty();
            }

            @Override
            public Class<?> next() {
                if (hasNext()) {
                    Class<?> poll = queue.poll();
                    assert poll != null;

                    Optional.ofNullable(poll.getSuperclass())
                            .ifPresent(queue::add);
                    queue.addAll(Arrays.asList(poll.getInterfaces()));
                    return poll;
                } else {
                    throw new IndexOutOfBoundsException("No more classes available!");
                }
            }
        }, Spliterator.DISTINCT);
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
        } catch (ClassNotFoundException e) {
            return false;
        }

        return true;
    }

    private static <T extends Annotation> Optional<T> annotation(
            Class<?> type, Class<T> annotationType
    ) {
        if (type.isAnnotationPresent(annotationType)) {
            return Optional.ofNullable(type.getAnnotation(annotationType));
        }

        return Optional.empty();
    }

    public static Stream<Method> externalMethodsAbove(Class<?> above, Class<?> startingFrom) {
        return Arrays.stream(above.getMethods())
                .filter(mtd -> !mtd.getDeclaringClass().isAssignableFrom(above));
    }

    public static boolean matchingFootprint(Class<?>[] param1, Class<?>[] param2) {
        for (int i = 0; i < param1.length && i < param2.length; i++) {
            if (!typeCompat(param1[i], param2[i]))
                return false;
        }

        return true;
    }

    public static <T> @Nullable T forceGetField(Object from, String fieldName) {
        final Class<?> kls = from.getClass();

        try {
            final Field field = kls.getDeclaredField(fieldName);

            if (!field.isAccessible())
                field.setAccessible(true);

            return Polyfill.uncheckedCast(field.get(from));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
}
