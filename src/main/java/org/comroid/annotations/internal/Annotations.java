package org.comroid.annotations.internal;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.comroid.annotations.Alias;
import org.comroid.annotations.Convert;
import org.comroid.annotations.Ignore;
import org.comroid.api.data.seri.DataStructure;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.info.Constraint;
import org.comroid.api.java.ReflectionHelper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.*;
import static java.util.stream.Stream.*;
import static java.util.stream.Stream.of;
import static org.comroid.api.func.util.Streams.*;

@SuppressWarnings({"DuplicatedCode","BooleanMethodIsAlwaysInverted"})
@Log
@UtilityClass
@ApiStatus.Internal
public class Annotations {
    public static final Class<?>[] SystemFilters = new Class<?>[]{Object.class, Class.class, Annotation.class};

    @ApiStatus.Experimental
    @Convert(identifyVia = "annotationType")
    public Constraint.API expect(AnnotatedElement context) {
        return Constraint.fail();
    }

    public Set<String> aliases(@NotNull AnnotatedElement of) {
        return findAnnotations(Alias.class, of)
                .filter(alias -> alias.context.getClass().equals(of.getClass()))
                .flatMap(it -> stream(it.annotation.value()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean ignore(@NotNull AnnotatedElement it, @NotNull Class<?> context) {
        var yield = findAnnotations(Ignore.class, it)
                .filter(result -> !ignoreAncestors(result.getContext(), Ignore.class))
                .findFirst();
        if (yield.isEmpty())
            return false;
        var anno = yield.get();
        var types = anno.annotation.value();
        if (types.length == 0)
            return true;
        return asList(types).contains(context);
    }

    public <A extends Annotation> Stream<Result<A>> findAnnotations(final Class<A> type, final AnnotatedElement target) {
        Constraint.Type.anyOf(target, "target", Class.class, Member.class).run();

        // @Ignore should inherit upwards indefinitely; unless specified otherwise with @Ignore.Ancestors
        // @Alias should inherit only between ancestors of same type

        final var typeInherit = Wrap.of(type.getAnnotation(Inherit.class));
        final var decl = ReflectionHelper.declaringClass(target);

        // do not scan system classes
        if (decl.getPackageName().startsWith("java"))
            return empty();

        // collect members
        return of(target).flatMap(member -> {
            var inherit = typeInherit.orRef(() -> Wrap.of(member.getAnnotation(Inherit.class)))
                    .map(Inherit::value)
                    .orElse(Inherit.Type.Default);

            // expand with ancestors by local or annotations @Inheritance annotations
            var sources = of(member);
            switch (inherit) {
                case None:
                    break;
                case FromSupertype, FromBoth:
                    sources = sources.collect(append(findAncestor(member, type).stream()));
                case FromParent:
                    sources = sources.collect(append(decl));
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + inherit);
            }

            // get most relevant annotation
            return sources.flatMap(mem -> {
                while (mem != null && !mem.isAnnotationPresent(type)) {
                    Wrap<AnnotatedElement> ancestor = findAncestor(mem, type);
                    if (ancestor.isNull())
                        break;
                    mem = ancestor.orElse(null);
                }
                if (mem == null)
                    throw new AssertionError();
                if (!mem.isAnnotationPresent(type))
                    return empty();
                return of(new Result<>(mem.getAnnotation(type), mem, member, decl));
            });
        });
    }

    public boolean ignoreAncestors(AnnotatedElement target, Class<? extends Annotation> goal) {
        Constraint.Type.anyOf(target, "target", Class.class, Member.class).run();

        if (target instanceof Constructor<?>)
            return true;
        return findAnnotations(Ignore.Ancestor.class, target)
                .map(Result::getAnnotation)
                .anyMatch(anno -> {
                    var goals = anno.value();
                    return goals.length == 0 || asList(goals).contains(goal);
                });
    }

    @SuppressWarnings("ConstantValue") // false positive
    private Wrap<AnnotatedElement> findAncestor(AnnotatedElement target, Class<? extends Annotation> goal) {
        Constraint.Type.anyOf(target, "target", Class.class, Member.class).run();

        if (goal.equals(Ignore.Ancestor.class) || ignoreAncestors(target, goal)) {
            log.fine("goal parameter for findAncestor() should never be typeof Ignore.Ancestor");
            return Wrap.empty();
        }

        Class<?> decl;
        if (target instanceof Class<?>) {
            decl = (Class<?>) target;
        } else if (target instanceof Member mem) {
            decl = mem.getDeclaringClass();
        } else throw new AssertionError("Invalid element: " + target);
        if (target instanceof Class<?> && decl.getPackageName().startsWith("java"))
            return Wrap.empty();

        try {
            if (target instanceof Member mem) {
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
            } else if (target instanceof Class<?> cls)
                return Wrap.of(cls.getSuperclass());
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            return Wrap.empty();
        }
        throw new IllegalArgumentException("Invalid element: " + target);
    }

    public <T extends Member & AnnotatedElement> String toString(Expect expect, T member) {
        return "%s.%s does not return %s for Annotations.%s()".formatted(
                member.getDeclaringClass().getSimpleName(), member.getName(), expect.value(), expect.onTarget());
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Result<A extends Annotation> implements Annotation {
        @NotNull
        @Ignore
        A annotation;
        @NotNull AnnotatedElement context;
        @NotNull AnnotatedElement annotated;
        @NotNull Class<?> declarator;

        @Override
        public Class<? extends Annotation> annotationType() {
            return annotation.annotationType();
        }
    }
}