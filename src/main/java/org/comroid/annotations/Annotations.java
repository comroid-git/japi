package org.comroid.annotations;

import lombok.experimental.UtilityClass;
import org.comroid.api.SupplierX;
import org.comroid.util.Constraint;
import org.comroid.util.Streams;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.*;
import static java.util.stream.Stream.*;
import static org.comroid.util.Streams.*;

@SuppressWarnings({"DuplicatedCode","BooleanMethodIsAlwaysInverted"})
@UtilityClass
@ApiStatus.Internal
public class Annotations {
    public Set<String> aliases(@NotNull AnnotatedElement of) {
        return Stream.of(of.getAnnotation(Alias.class))
                .filter(Objects::nonNull)
                .flatMap(it -> stream(it.value()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean ignore(@NotNull AnnotatedElement it, @NotNull Class<?> context) {
        var yield = findAnnotations(Ignore.class, it).findFirst();
        if (yield.isEmpty())
            return false;
        var anno = yield.get();
        var types = anno.value();
        if (types.length == 0)
            return true;
        return asList(types).contains(context);
    }

    public <A extends Annotation> Stream<A> findAnnotations(final Class<A> type, final AnnotatedElement in) {
        Constraint.Type.anyOf(in, "in", Class.class, Member.class).run();

        Class<?> decl;
        if (in instanceof Class<?>) {
            decl = (Class<?>) in;
        } else if (in instanceof Member) {
            decl = ((Member) in).getDeclaringClass();
        } else throw new IllegalArgumentException("Invalid element: " + in);
        if (decl.getPackageName().startsWith("java"))
            return empty();

        // first, try get annotation from type
        return of(in.getAnnotation(type))
                .filter(Objects::nonNull)
                // otherwise, recurse into ancestors
                .collect(append(ignoreAncestors(in, type)
                        ? empty()
                        : (in instanceof Class<?>
                        // for class, try supertypes
                        ? of(decl).flatMap(c -> concat(
                        of(c.getSuperclass()),
                        stream(c.getInterfaces())))
                        // for members, try declaring class first
                        : of(decl)
                        // then if applicable, try return types next
                        .collect(append(of(in).map(x -> {
                            if (x instanceof Field)
                                return ((Field) x).getType();
                            else if (x instanceof Method)
                                return ((Method) x).getReturnType();
                            else return null;
                        })))
                        // then otherwise, try ancestors
                        .collect(append(of(in).flatMap(x -> findAncestor(x, type).stream()))))
                        // cleanup
                        .filter(Objects::nonNull)
                        .distinct()
                        // recurse
                        .flatMap(x -> findAnnotations(type, x))));
    }

    public boolean ignoreAncestors(AnnotatedElement of, Class<? extends Annotation> goal) {
        Constraint.Type.anyOf(of, "of", Class.class, Member.class).run();

        if (of instanceof Constructor<?>)
            return true;
        Class<?> decl;
        if (of instanceof Class<?>) {
            decl = (Class<?>) of;
        } else if (of instanceof Member) {
            decl = ((Member) of).getDeclaringClass();
        } else throw new AssertionError("Invalid element: " + of);
        if (of instanceof Class<?> && decl.getPackageName().startsWith("java"))
            return true;
        if (of.isAnnotationPresent(Ignore.Ancestor.class)) {
            var anno = of.getAnnotation(Ignore.Ancestor.class);
            var goals = anno.value();
            return goals.length == 0 || asList(goals).contains(goal);
        }
        return false;
    }

    @SuppressWarnings("ConstantValue") // false positive
    public SupplierX<AnnotatedElement> findAncestor(AnnotatedElement of, Class<? extends Annotation> goal) {
        Constraint.Type.anyOf(of, "of", Class.class, Member.class).run();
        if (ignoreAncestors(of, goal))
            return SupplierX.empty();

        Class<?> decl;
        if (of instanceof Class<?>) {
            decl = (Class<?>) of;
        } else if (of instanceof Member) {
            decl = ((Member) of).getDeclaringClass();
        } else throw new AssertionError("Invalid element: " + of);
        if (of instanceof Class<?> && decl.getPackageName().startsWith("java"))
            return SupplierX.empty();

        try {
            if (of instanceof Member) {
                var pType = ((Member) of).getDeclaringClass().getSuperclass();
                if (pType == null)
                    return SupplierX.empty();
                // todo: this is inaccurate for different parameter overrides
                if (of instanceof Method)
                    return SupplierX.of(pType.getDeclaredMethod(((Method) of).getName(), ((Method) of).getParameterTypes()));
                else if (of instanceof Field)
                    return SupplierX.of(pType.getDeclaredField(((Field) of).getName()));
                else if (of instanceof Constructor<?>)
                    return SupplierX.of(pType.getDeclaredConstructor(((Constructor<?>) of).getParameterTypes()));
            } else if (of instanceof Class<?>)
                return SupplierX.of(((Class<?>) of).getSuperclass());
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            return SupplierX.empty();
        }
        throw new IllegalArgumentException("Invalid element: " + of);
    }
}