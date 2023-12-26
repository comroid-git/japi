package org.comroid.annotations;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.comroid.api.DataStructure;
import org.comroid.api.Wrap;
import org.comroid.util.Constraint;
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
        return findAnnotations(Alias.class, of)
                .filter(alias -> alias.context.getClass().equals(of.getClass()))
                .flatMap(it -> stream(it.annotation.value()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean ignore(@NotNull AnnotatedElement it, @NotNull Class<?> context) {
        var yield = findAnnotations(Ignore.class, it).findFirst();
        if (yield.isEmpty())
            return false;
        var anno = yield.get();
        var types = anno.annotation.value();
        if (types.length == 0)
            return true;
        return asList(types).contains(context);
    }

    public <A extends Annotation> Stream<Result<A>> findAnnotations(final Class<A> type, final AnnotatedElement in) {
        Constraint.Type.anyOf(in, "in", Class.class, Member.class).run();

        Class<?> decl;
        if (in instanceof Class<?>) {
            decl = (Class<?>) in;
        } else if (in instanceof Member mem) {
            decl = mem.getDeclaringClass();
        } else throw new IllegalArgumentException("Invalid element: " + in);
        if (decl.getPackageName().startsWith("java"))
            return empty();

        // first, try get annotation from type
        return of(in.getAnnotations())
                .flatMap(cast(type))
                .map(a -> new Result<>(a, in, decl))
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
                            if (x instanceof Field fld)
                                return fld.getType();
                            else if (x instanceof Method mtd)
                                return mtd.getReturnType();
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
        } else if (of instanceof Member mem) {
            decl = mem.getDeclaringClass();
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
    public Wrap<AnnotatedElement> findAncestor(AnnotatedElement of, Class<? extends Annotation> goal) {
        Constraint.Type.anyOf(of, "of", Class.class, Member.class).run();
        if (ignoreAncestors(of, goal))
            return Wrap.empty();

        Class<?> decl;
        if (of instanceof Class<?>) {
            decl = (Class<?>) of;
        } else if (of instanceof Member mem) {
            decl = mem.getDeclaringClass();
        } else throw new AssertionError("Invalid element: " + of);
        if (of instanceof Class<?> && decl.getPackageName().startsWith("java"))
            return Wrap.empty();

        try {
            if (of instanceof Member mem) {
                Member chk;
                if (mem instanceof DataStructure.Member dmem)
                    if (dmem.getContext() instanceof Parameter)
                        return Wrap.empty();
                    else chk = (Member) dmem.getContext();
                else chk = mem;
                var pType = chk.getDeclaringClass().getSuperclass();
                if (pType == null)
                    return Wrap.empty();
                // todo: this is inaccurate for different parameter overrides
                if (chk instanceof Method mtd)
                    return Wrap.of(pType.getDeclaredMethod(mtd.getName(), mtd.getParameterTypes()));
                else if (chk instanceof Field fld)
                    return Wrap.of(pType.getDeclaredField(fld.getName()));
                else if (chk instanceof Constructor<?> ctor)
                    return Wrap.of(pType.getDeclaredConstructor(ctor.getParameterTypes()));
            } else if (of instanceof Class<?> cls)
                return Wrap.of(cls.getSuperclass());
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            return Wrap.empty();
        }
        throw new IllegalArgumentException("Invalid element: " + of);
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Result<A extends Annotation> implements Annotation {
        @NotNull A annotation;
        @NotNull AnnotatedElement context;
        @NotNull Class<?> declarator;

        @Override
        public Class<? extends Annotation> annotationType() {
            return annotation.annotationType();
        }
    }
}