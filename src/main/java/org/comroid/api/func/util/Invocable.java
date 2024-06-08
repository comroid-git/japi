package org.comroid.api.func.util;

import org.comroid.annotations.Ignore;
import org.comroid.annotations.OptionalVararg;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.Named;
import org.comroid.api.func.Provider;
import org.comroid.api.func.exc.ThrowingFunction;
import org.comroid.api.func.exc.ThrowingSupplier;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.info.Log;
import org.comroid.api.java.ReflectionHelper;
import org.comroid.exception.RethrownException;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Stream;

@Ignore
@Experimental
public interface Invocable<T> extends Named {
    static <T, E extends Throwable> Invocable<T> ofCallable(
            ThrowingSupplier<T, E> callable
    ) {
        return ofProvider((Provider.Now<T>) () -> {
            try {
                return callable.get();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    static <T> Invocable<T> ofProvider(Provider<T> provider) {
        return new Support.OfProvider<>(provider);
    }

    static <T> Invocable<T> ofConsumer(Class<T> type, Consumer<T> consumer) {
        return new Support.OfConsumer<>(type, consumer);
    }

    static <T> Invocable<T> ofFieldGet(final Field field) {
        return ofFieldGet(null, field);
    }

    static <T> Invocable<T> ofFieldGet(final @Nullable Object target, final Field field) {
        return new Invocable<>() {
            @Override
            public @Nullable AccessibleObject accessor() {
                return field;
            }

            @Override
            public Class<?>[] parameterTypesOrdered() {
                return new Class[0];
            }

            @Override
            public @Nullable T invoke(@Nullable Object tgt, Object... args) throws IllegalAccessException {
                return Polyfill.uncheckedCast(field.get(Wrap.of(tgt).orElse(target)));
            }
        };
    }

    static <T> Invocable<@Nullable T> ofFieldSet(final Field field) {
        return ofFieldSet(null, field);
    }

    static <T> Invocable<@Nullable T> ofFieldSet(final @Nullable Object target, final Field field) {
        return new Invocable<>() {
            @Override
            public @Nullable AccessibleObject accessor() {
                return field;
            }

            @Override
            public Class<?>[] parameterTypesOrdered() {
                return new Class[]{field.getType()};
            }

            @Override
            public @Nullable T invoke(@Nullable Object tgt, Object... args) throws IllegalAccessException {
                try {
                    field.set(Objects.requireNonNullElse(tgt, target), args[0]);
                } catch (IllegalArgumentException t){
                    Log.at(Level.WARNING, "Could not set field " + field.getName(), t);
                }
                return null;
            }
        };
    }

    static <T> Invocable<T> ofExecutable(Executable executable) {
        return ofExecutable(null, executable);
    }

    static <T> Invocable<T> ofExecutable(@Nullable Object target, Executable executable) {
        if (executable instanceof Method)
            return ofMethodCall(target, (Method) executable);
        if (executable instanceof Constructor) //noinspection unchecked
            return ofConstructor((Constructor<T>) executable);
        throw new IllegalArgumentException("Unknown type: " + executable);
    }

    static <T> Invocable<T> ofMethodCall(Class<?> inClass, String methodName) {
        return ofMethodCall(null, inClass, methodName);
    }

    static <T> Invocable<T> ofMethodCall(@NotNull Object target, String methodName) {
        return ofMethodCall(target, target.getClass(), methodName);
    }

    static <T> Invocable<T> ofMethodCall(@Nullable Object target, Class<?> inClass, String methodName) {
        return Arrays.stream(inClass.getMethods())
                .filter(mtd -> mtd.getName().equals(methodName))
                .findAny()
                .map(mtd -> Invocable.<T>ofMethodCall(target, mtd))
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Class %s does not have a method named %s", inClass, methodName)));
    }

    static <T> Invocable<T> ofMethodCall(Method method) {
        return ofMethodCall(null, method);
    }

    static <T> Invocable<T> ofMethodCall(@Nullable Object target, Method method) {
        return new Support.OfMethod<>(method, target);
    }

    static <T> Invocable<T> ofConstructor(Class<T> type, @OptionalVararg Class<?>... params) {
        Constructor<?>[] constructors = type.getConstructors();

        if (constructors.length > 1) {
            return Arrays.stream(constructors)
                    .filter(it -> it.getParameterCount() == params.length)
                    .filter(it -> ReflectionHelper.matchingFootprint(it.getParameterTypes(), params))
                    .findAny()
                    .map(it -> Invocable.<T>ofConstructor(Polyfill.uncheckedCast(it)))
                    .orElseThrow(() -> new NoSuchElementException("No suitable constructor could be found in " + type));
        } else {
            return ofConstructor(ReflectionHelper.findConstructor(type, params)
                    .orElseThrow(() -> new NoSuchElementException("No suitable constructor found in " + type)));
        }
    }

    static <T> Invocable<T> ofConstructor(Constructor<T> constructor) {
        return new Support.OfConstructor<>(constructor);
    }

    static <T> Invocable<T> ofClass(Class<? extends T> type) {
        return new Support.OfClass<>(type);
    }

    static <T> Invocable<T> paramReturning(Class<T> type) {
        return new Support.ParamReturning<>(type);
    }

    static <T> Invocable<T> constant(T value) {
        //noinspection unchecked
        return (Invocable<T>) Support.Constant.Cache.computeIfAbsent(value, Support.Constant::new);
    }

    static <T> Invocable<T> empty() {
        //noinspection unchecked
        return (Invocable<T>) Support.Empty;
    }

    static <T> T newInstance(Class<? extends T> type, Object... args) {
        return ReflectionHelper.findConstructor(type, ReflectionHelper.types(args))
                .map(ThrowingFunction.rethrowing(constr -> {
                    final Object[] arrange;
                    try {
                        arrange = ReflectionHelper.arrange(args, constr.getParameterTypes());
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Could not construct " + type, e);
                    }
                    return constr.newInstance(arrange);
                }, RuntimeException::new))
                .orElseThrow(() -> new NoSuchElementException("Could not find a suitable constructor for type: " + type));
    }

    @Experimental
    static Throwable unwrapInvocationTargetException(Throwable t) {
        if (t instanceof InvocationTargetException || (t instanceof RuntimeException && t.getCause() instanceof InvocationTargetException))
            return unwrapInvocationTargetException(t.getCause());
        return t;
    }

    @Nullable
    default AccessibleObject accessor() {
        return null;
    }

    default boolean canAccess(Object target) {
        var func = accessor();
        return func != null && func.canAccess(target);
    }

    default boolean makeAccessible() {
        var func = accessor();
        if (func == null)
            return false;
        try {
            return func.trySetAccessible();
        } catch (InaccessibleObjectException ioe) {
            return false;
        }
    }

    Class<?>[] parameterTypesOrdered();

    @Nullable T invoke(@Nullable Object target, Object... args) throws InvocationTargetException, IllegalAccessException, InstantiationException;

    default @Nullable T invokeSilent(@Nullable Object target, Object... args) {
        try {
            return invoke(target, args);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RethrownException(e);
        }
    }

    default T invokeAutoOrder(Object... args) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        return invoke(null, tryArrange(args, parameterTypesOrdered()));
    }

    @Internal
    default Object[] tryArrange(Object[] args, Class<?>[] typesOrdered) {
        return tryArrange(args, typesOrdered, false);
    }

    @Internal
    default Object[] tryArrange(Object[] args, Class<?>[] typesOrdered, boolean simulate) {
        Object[] arranged;
        try {
            arranged = ReflectionHelper.arrange(args, typesOrdered);
        } catch (IllegalArgumentException iaEx) {
            if (simulate)
                return null;
            throw new IllegalArgumentException(String.format("unable to arrange arguments: %s - %s",
                    getName(), Arrays.toString(args)), iaEx);
        }
        return arranged;
    }

    default T invokeRethrow(Object... args) {
        try {
            return invoke(null, args);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    default <X extends Throwable> T invokeRethrow(Function<ReflectiveOperationException, X> remapper, Object... args) throws X {
        try {
            return invoke(null, args);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw remapper.apply(e);
        }
    }

    default T autoInvoke(Object... args) {
        try {
            return invokeAutoOrder(args);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    default @Nullable T silentAutoInvoke(Object... args) {
        try {
            return autoInvoke(args);
        } catch (Throwable ignored) {
            Log.at(Level.WARNING, "silentAutoInvoke() swallowed exception", ignored);
            return null;
        }
    }

    default TypeMap<T> typeMapped() {
        return this instanceof TypeMap ? (TypeMap<T>) this : TypeMap.boxed(this);
    }

    default Supplier<T> supplier() {
        class Adapter implements Supplier<T> {
            @Override
            public T get() {
                return autoInvoke();
            }
        }

        return new Adapter();
    }

    default <I> Function<I, T> function() {
        class Adapter implements Function<I, T> {
            @Override
            public T apply(I i) {
                return autoInvoke(i);
            }
        }

        return new Adapter();
    }

    default <I1, I2> BiFunction<I1, I2, T> biFunction() {
        class Adapter implements BiFunction<I1, I2, T> {
            @Override
            public T apply(I1 i1, I2 i2) {
                return autoInvoke(i1, i2);
            }
        }

        return new Adapter();
    }

    interface TypeMap<T> extends Invocable<T> {
        static Map<Class<?>, Object> mapArgs(Object... args) {
            final long distinct = Stream.of(args)
                    .map(Object::getClass)
                    .distinct()
                    .count();

            if (distinct != args.length)
                throw new IllegalArgumentException("Duplicate argument types detected");

            final Map<Class<?>, Object> yield = new HashMap<>();

            for (Object arg : args) {
                yield.put(arg.getClass(), arg);
            }

            return yield;
        }

        static <T> TypeMap<T> boxed(Invocable<T> invocable) {
            return new TypeMap<T>() {
                private final Invocable<T> underlying = invocable;

                @Override
                public String getName() {
                    return underlying.getName();
                }

                @Nullable
                @Override
                public T invoke(Map<Class<?>, Object> args) throws InvocationTargetException, IllegalAccessException, InstantiationException {
                    if (underlying instanceof Support.OfMethod) {
                        final Method method = ((Support.OfMethod<T>) underlying).method;
                        final Class<?>[] param = method.getParameterTypes();
                        final AnnotatedType[] annParam = method.getAnnotatedParameterTypes();

                        for (int i = 0; i < param.length; i++) {
                            final AnnotatedType annotated = annParam[i];
                            final Class<?> key = param[i];

                            if (args.containsKey(key) || !annotated.isAnnotationPresent(Null.class))
                                continue;
                            args.put(key, null);
                        }
                    }

                    return underlying.invokeAutoOrder(args.values().toArray());
                }

                @Override
                public @Nullable AccessibleObject accessor() {
                    return underlying.accessor();
                }

                @Override
                public Class<?>[] parameterTypesOrdered() {
                    return underlying.parameterTypesOrdered();
                }
            };
        }

        @Override
        default @Nullable T invoke(@Nullable Object target, Object... args) throws InvocationTargetException, IllegalAccessException, InstantiationException {
            return invoke(mapArgs(args));
        }

        @Nullable T invoke(Map<Class<?>, Object> args) throws InvocationTargetException, IllegalAccessException, InstantiationException;

        @Target(ElementType.PARAMETER)
        @Retention(RetentionPolicy.RUNTIME)
        @interface Null {
        }
    }

    @Experimental
    abstract class Magic<T> implements Invocable<T> {
        private final Invocable<T> underlying;

        @Override
        public String getName() {
            return underlying.getName();
        }

        protected Magic() {
            this.underlying = Invocable.ofMethodCall(this, ReflectionHelper.externalMethodsAbove(Magic.class, getClass())
                    .findAny()
                    .orElseThrow(() -> new NoSuchElementException("Could not find matching method")));
        }

        @Nullable
        @Override
        public T invoke(@Nullable Object target, Object... args) {
            return underlying.autoInvoke(args);
        }

        @Override
        public Class<?>[] parameterTypesOrdered() {
            return underlying.parameterTypesOrdered();
        }
    }

    @Internal
    final class Support {
        private static final Invocable<?> Empty = constant(null);

        private static final class OfProvider<T> implements Invocable<T> {
            private final Provider<T> provider;

            @Override
            public String getName() {
                return "OfProvider";
            }

            public OfProvider(Provider<T> provider) {
                this.provider = provider;
            }

            @Nullable
            @Override
            public T invoke(@Nullable Object target, Object... args) {
                return provider.now();
            }

            @Override
            public Class<?>[] parameterTypesOrdered() {
                return new Class[0];
            }
        }

        private static final class OfConstructor<T> implements Invocable<T> {
            private final Constructor<T> constructor;

            @Override
            public String getName() {
                return String.format("OfConstructor(%s)", constructor.getName());
            }

            public OfConstructor(Constructor<T> constructor) {
                this.constructor = constructor;
            }

            @Override
            public @NotNull T invoke(@Nullable Object target, Object... args) throws InvocationTargetException, IllegalAccessException {
                try {
                    return constructor.newInstance(args);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public @Nullable AccessibleObject accessor() {
                return constructor;
            }

            @Override
            public Class<?>[] parameterTypesOrdered() {
                return constructor.getParameterTypes();
            }
        }

        private static final class OfMethod<T> implements Invocable<T> {
            private final Method method;
            private final Object target;

            @Override
            public String getName() {
                return method.getName();
            }

            @Override
            public String getAlternateName() {
                return String.format("OfMethod(%s @ %s)", method.getName(), target);
            }

            private OfMethod(Method method, @Nullable Object target) {
                /*
                if (target == null && !Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalArgumentException("Target cannot be null on non-static methods!",
                            new NullPointerException()
                    );
                }
                 */

                this.method = method;
                this.target = target;
            }

            @Nullable
            @Override
            public T invoke(@Nullable Object target, Object... args) throws InvocationTargetException, IllegalAccessException {
                //noinspection unchecked
                return (T) method.invoke(target == null ? this.target : target, args);
            }

            @Override
            public @Nullable AccessibleObject accessor() {
                return method;
            }

            @Override
            public Class<?>[] parameterTypesOrdered() {
                return method.getParameterTypes();
            }
        }

        private static final class ParamReturning<T> implements Invocable<T> {
            private final Class<T> type;
            private final Class<?>[] typeArray;

            @Override
            public String getName() {
                return "Returning" + type.getSimpleName();
            }

            private ParamReturning(Class<T> type) {
                this.type = type;
                this.typeArray = new Class[]{type};
            }

            @Nullable
            @Override
            public T invoke(@Nullable Object target, Object... args) {
                //noinspection unchecked
                return Stream.of(args)
                        .filter(type::isInstance)
                        .findAny()
                        .map(it -> (T) it)
                        .orElseThrow(() -> new NoSuchElementException(String.format("No parameter with type %s given",
                                type.getName()
                        )));
            }

            @Override
            public Class<?>[] parameterTypesOrdered() {
                return typeArray;
            }
        }

        private static final class Constant<T> implements Invocable<T> {
            private static final Map<Object, Invocable<Object>> Cache = new ConcurrentHashMap<>();
            private final T value;

            @Override
            public String getName() {
                return String.format("Constant(%s)", value);
            }

            private Constant(T value) {
                this.value = value;
            }

            @Nullable
            @Override
            public T invoke(@Nullable Object target, Object... args) {
                return value;
            }

            @Override
            public Class<?>[] parameterTypesOrdered() {
                return new Class[0];
            }
        }

        private static final class OfConsumer<T> implements Invocable<T> {
            private final Class<T> argType;
            private final Consumer<T> consumer;
            private final Class<?>[] argTypeArr;

            @Override
            public String getName() {
                return String.format("OfConsumer(%s)", argType.getName());
            }

            private OfConsumer(Class<T> argType, Consumer<T> consumer) {
                this.argType = argType;
                this.consumer = consumer;
                this.argTypeArr = new Class[]{argType};
            }

            @Nullable
            @Override
            public T invoke(@Nullable Object target, Object... args) {
                if (argType.isInstance(args[0])) {
                    consumer.accept(argType.cast(args[0]));
                    return null;
                } else {
                    throw new IllegalArgumentException(String.format("Invalid Type: %s",
                            args[0].getClass()
                                    .getName()
                    ));
                }
            }

            @Override
            public Class<?>[] parameterTypesOrdered() {
                return argTypeArr;
            }
        }

        private static final class OfClass<T> implements Invocable<T> {
            private final Class<? extends T> type;

            @Override
            public String getName() {
                return String.format("OfClass(%s)", type.getName());
            }

            private OfClass(Class<? extends T> type) {
                this.type = type;
            }

            @Override
            public Class<?>[] parameterTypesOrdered() {
                return Stream.of(type.getConstructors())
                        .min(Comparator.comparingInt(Constructor::getParameterCount))
                        .map(Constructor::getParameterTypes)
                        .orElseGet(() -> new Class[0]);
            }

            @Override
            public @Nullable T invoke(@Nullable Object target, Object... args) throws InvocationTargetException, IllegalAccessException, InstantiationException {
                final Constructor<? extends T> constructor = ReflectionHelper
                        .findConstructor(type, ReflectionHelper.types(args))
                        .orElse(null);

                if (constructor == null)
                    return null;
                return constructor.newInstance(tryArrange(args, constructor.getParameterTypes()));
            }
        }
    }
}
