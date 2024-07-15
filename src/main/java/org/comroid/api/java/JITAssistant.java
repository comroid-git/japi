package org.comroid.api.java;

import org.comroid.api.Polyfill;
import org.comroid.api.func.util.Invocable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@ApiStatus.Experimental
public final class JITAssistant {
    public static CompletableFuture<Void> prepare(Class<?>... classes) {
        return CompletableFuture.allOf(Arrays.stream(classes)
                                               .map(type -> {
                                                   Object instance;

                                                   try {
                                                       instance = ReflectionHelper.instance(type);
                                                   } catch (Throwable t) {
                                                       instance = null;
                                                   }

                                                   return prepareInstance(instance, type);
                                               })
                                               .toArray(CompletableFuture[]::new));
    }

    public static CompletableFuture<Void> prepareInstance(Object target, Class<?> klaas) {
        return CompletableFuture.supplyAsync(() -> {
            final Stream<Invocable<Object>> methods = Arrays.stream(klaas.getMethods())
                    .filter(method -> target != null || Modifier.isStatic(method.getModifiers()))
                    .map(method -> Invocable.ofMethodCall(target, method));
            final Stream<Invocable<Object>> fields = Arrays.stream(klaas.getFields())
                    .filter(field -> target != null || Modifier.isStatic(field.getModifiers()))
                    .map(field -> Invocable.ofCallable(() -> field.get(target)));

            Stream.concat(methods, fields).forEach(Invocable::silentAutoInvoke);

            return null;
        });
    }

    public static CompletableFuture<Void> prepareStatic(Class<?>... classes) {
        return CompletableFuture.allOf(Arrays.stream(classes)
                                               .flatMap(type -> Stream.concat(
                                                       Stream.of(type.getFields()),
                                                       Stream.of(type.getMethods())
                                                               .filter(method -> method.getParameterCount() == 0)))
                                               .filter(it -> Modifier.isStatic(it.getModifiers()) && it.isAccessible())
                                               .map(it -> buildCallable(it, null))
                                               .map(objectCallable -> {
                                                   try {
                                                       return CompletableFuture.completedFuture(objectCallable.call());
                                                   } catch (Throwable t) {
                                                       return Polyfill.failedFuture(t);
                                                   }
                                               })
                                               .toArray(CompletableFuture[]::new));
    }

    @Internal
    @SuppressWarnings("unchecked")
    private static <T> Callable<T> buildCallable(AccessibleObject accessible, Object target) {
        if (accessible instanceof Field)
            return () -> (T) ((Field) accessible).get(target);
        if (accessible instanceof Method)
            return () -> (T) ((Method) accessible).invoke(target);

        throw new IllegalArgumentException(accessible.getClass().getName());
    }
}
