package org.comroid.api.java;

import lombok.SneakyThrows;
import org.comroid.annotations.Instance;
import org.comroid.api.Polyfill;
import org.comroid.api.func.ext.Wrap;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.reflect.Modifier.*;

@ApiStatus.Experimental
public final class ReflectionHelper {
    @SneakyThrows
    public static <T> T call(Object target, String methodName, Object... args) {
        boolean dynamic = !(target instanceof Class);
        var cls = dynamic ? target.getClass() : ((Class<?>) target);
        return Polyfill.uncheckedCast(cls
                .getMethod(methodName, types(args))
                .invoke(dynamic ? target : null, args));
    }

    public static Class<?>[] types(Object... args) {
        final Class<?>[] yields = new Class[args.length];

        for (int i = 0; i < args.length; i++) {
            yields[i] = args[i].getClass();
        }

        return yields;
    }

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
                        field.setAccessible(true);
                        return (T) field.get(null);
                    } catch (IllegalAccessException e) {
                        throw new AssertionError("Cannot access field", e);
                    }
                });
    }

    public static <T> Optional<Constructor<T>> findConstructor(Class<T> inClass, Class<?>[] types) {
        final Constructor<?>[] constructors = inClass.getDeclaredConstructors();

        if (constructors.length == 0) {
            return Optional.empty();
        }
        // todo Fix this
        return Stream.of(constructors)
                .map(it -> (Constructor<T>) it)
                .max(Comparator.comparingLong(constr -> Stream.of(constr.getParameterTypes())
                        .filter(typ -> Stream.of(types).anyMatch(typ::isAssignableFrom))
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
            Class<?> type, Class<? extends Annotation> annotationType
    ) {
        return Arrays.stream(type.getFields())
                .filter(prop -> prop.isAnnotationPresent(annotationType))
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<Method> methodsWithAnnotation(
            Class<?> type, Class<? extends Annotation> instanceClass
    ) {
        Set<Method> yields = new HashSet<>();

        for (Method method : type.getMethods()) {
            if (method.isAnnotationPresent(instanceClass)) {
                yields.add(method);
            }
        }

        return yields;
    }

    public static boolean typeCompat(Class<?> expected, Class<?> target) {
        return expected.getName().contains(".")
               ? expected.equals(target) || expected.isAssignableFrom(target)
               : switch (expected.getName()) {
                   case "int" -> typeCompat(Integer.class, target);
                   case "double" -> typeCompat(Double.class, target);
                   case "long" -> typeCompat(Long.class, target);
                   case "char" -> typeCompat(Character.class, target);
                   case "boolean" -> typeCompat(Boolean.class, target);
                   case "short" -> typeCompat(Short.class, target);
                   case "float" -> typeCompat(Float.class, target);
                   default -> expected.isAssignableFrom(target);
               };
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

    public static Object[] arrange(Object[] args, Class<?>[] typesOrdered) throws IllegalArgumentException {
        final Object[] yields = new Object[typesOrdered.length];

        for (int i = 0; i < typesOrdered.length; i++) {
            int finalli = i;
            yields[i] = Stream.of(args)
                    .filter(Objects::nonNull)
                    .filter(it -> typeCompat(typesOrdered[finalli], it.getClass()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
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

    public static <T> @Nullable T forceGetField(Object from, Class<? super T> type) {
        return Arrays.stream(from.getClass().getFields())
                .filter(fld -> type.isAssignableFrom(fld.getType()))
                .findAny()
                .map(fld -> forceGetField(from, fld))
                .map(Polyfill::<T>uncheckedCast)
                .orElse(null);
    }

    public static <T> @Nullable T forceGetField(Object from, Field field) {
        try {
            if (!field.canAccess(from))
                field.setAccessible(true);
            return Polyfill.uncheckedCast(field.get(isStatic(field.getModifiers()) ? null : from));
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static <T> @Nullable T forceGetField(Object from, String fieldName) {
        try {
            final Class<?> kls   = from.getClass();
            final Field    field = kls.getDeclaredField(fieldName);
            return forceGetField(from, field);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    public static <T> T fieldByName(Class<?> inClass, Object instance, String fieldName, Class<T> cast) {
        try {
            final Field field = inClass.getDeclaredField(fieldName);

            if (instance == null && Modifier.isStatic(field.getModifiers()))
                throw new IllegalArgumentException("Instance missing for non-static field");

            Object yield = field.get(instance);
            return cast.cast(yield);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int extendingClassesCount(Class<?> inClass, Class<?> target) {
        Stream<Class<?>> concat1 = Stream.concat(Stream.of(inClass.getClasses()), Stream.of(inClass.getInterfaces()));
        return (int) Stream.concat(concat1, Stream.of(inClass.getSuperclass()))
                .filter(target::isAssignableFrom)
                .count();
    }

    public static String simpleClassName(Class<?> cls) {
        return cls == null ? "" : (simpleClassName(cls.getDeclaringClass()) + (cls.getDeclaringClass() == null ? "" : '.') + cls.getSimpleName());
    }

    public static <T> Wrap<T> obtainInstance(Class<T> targetClass, Object... args) {
        return Wrap.ofOptional(ReflectionHelper.instanceField(targetClass))
                .or(() -> Polyfill.uncheckedCast(ReflectionHelper.instance(targetClass, args)));
    }

    public static <T> T resolveField(String fieldName) {
        return resolveField(fieldName, null);
    }

    public static <T> T resolveField(String fieldName, @Nullable Object target) {
        try {
            int      li    = fieldName.lastIndexOf('.');
            Class<?> in    = Class.forName(fieldName.substring(0, li));
            Field    field = in.getField(fieldName.substring(li + 1));
            //noinspection unchecked
            return (T) field.get(target);
        } catch (Throwable e) {
            //Polyfill.COMMON_LOGGER.error("error in resolveField", e);
            throw new RuntimeException(e);
        }
    }

    public static Class<?> declaringClass(AnnotatedElement context) {
        if (context instanceof Class<?> cls) return cls;
        else if (context instanceof Member mem) return mem.getDeclaringClass();
        else if (context instanceof Parameter param) return declaringClass(param.getDeclaringExecutable());
        throw new IllegalArgumentException("Unknown context: " + context);
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

    @ApiStatus.Experimental
    private static <T> T eval(@Language("java") String expr) {
        throw new UnsupportedOperationException("Not yet implemented"); // todo
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
}
