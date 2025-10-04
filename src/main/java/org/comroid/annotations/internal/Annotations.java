package org.comroid.annotations.internal;

import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.java.Log;
import org.comroid.annotations.Alias;
import org.comroid.annotations.Category;
import org.comroid.annotations.Convert;
import org.comroid.annotations.Default;
import org.comroid.annotations.Description;
import org.comroid.annotations.Ignore;
import org.comroid.annotations.Readonly;
import org.comroid.annotations.Related;
import org.comroid.api.Polyfill;
import org.comroid.api.data.bind.DataStructure;
import org.comroid.api.data.seri.type.StandardValueType;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Debug;
import org.comroid.api.func.util.Invocable;
import org.comroid.api.info.Constraint;
import org.comroid.api.java.ReflectionHelper;
import org.comroid.api.java.SoftDepend;
import org.comroid.api.text.Translation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.*;
import static java.util.Arrays.*;
import static java.util.stream.Stream.*;
import static java.util.stream.Stream.of;
import static org.comroid.api.func.util.Streams.*;

@Log
@ApiStatus.Internal
@SuppressWarnings({ "DuplicatedCode", "BooleanMethodIsAlwaysInverted" })
public class Annotations {
    public static boolean readonly(AnnotatedElement element) {
        return concat(of(Readonly.class),
                SoftDepend.<Annotation>type("jakarta.persistence.Id").stream()).flatMap(type -> findAnnotations(type,
                        element))
                .findAny()
                .isPresent();
    }

    public static final Class<?>[] SystemFilters = new Class<?>[]{ Object.class, Class.class, Annotation.class };

    @ApiStatus.Experimental
    @Convert(identifyVia = "annotationType")
    public static Constraint.API expect(AnnotatedElement context) {
        return Constraint.fail();
    }

    private Annotations() {
        throw new UnsupportedOperationException();
    }

    public static Set<String> aliases(@NotNull AnnotatedElement of) {
        return findAnnotations(Alias.class, of).filter(alias -> alias.context.getClass().equals(of.getClass()))
                .flatMap(it -> stream(it.annotation.value()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Stream<Result<Description>> description(@NotNull AnnotatedElement of) {
        return findAnnotations(Description.class, of).sorted(Comparator.comparing(Result::getAnnotation, Description.COMPARATOR));
    }

    public static String descriptionText(@NotNull AnnotatedElement of) {
        return toString(description(of).map(Result::getAnnotation).toArray(Description[]::new));
    }

    public static Wrap<Category.Adapter> category(@NotNull AnnotatedElement of) {
        return Wrap.ofStream(findAnnotations(Category.class, of)).map(Result::getAnnotation).map(Category.Adapter::wrap);
    }

    public static <R> @Nullable R defaultValue(@NotNull AnnotatedElement of) {
        var expr = findAnnotations(Default.class, of).map(Result::getAnnotation).map(Default::value).toList();
        try {
            final var silent = new Object() {
                @SneakyThrows
                public static void throwIfExcPresent(SnippetEvent e) {
                    var exc = e.exception();
                    if (exc != null) throw exc;
                }
            };
            try (final var jShell = JShell.create()) {
                return expr.stream()
                        .flatMap(code -> jShell.eval(code).stream())
                        .peek(e -> silent.throwIfExcPresent(e))
                        .map(SnippetEvent::value)
                        .filter(Objects::nonNull)
                        .findAny()
                        .map(StandardValueType::findGoodType)
                        .map(Polyfill::<R>uncheckedCast)
                        .orElse(null);
            }
        } catch (Throwable t) {
            log.log(Debug.isDebug() ? Level.WARNING : Level.FINE, "Failed to evaluate default expression of " + of, t);

            // attempt to parse using SVT
            var parse    = String.join("", expr);
            var goodType = StandardValueType.findGoodType(parse);
            return Polyfill.uncheckedCast(goodType);
        }
    }

    public static <T> Stream<T> constants(Class<? extends T> type) {
        if (type.isEnum()) return of(type.getEnumConstants());
        return of(type.getDeclaredFields()).filter(fld -> {
                    final int mod = fld.getModifiers();
                    return isStatic(mod) && isPublic(mod) && fld.getType().isAssignableFrom(type);
                }).map(fld -> Invocable.ofFieldGet(fld).invokeRethrow())
                // do we really need to go through supertypes recursively?
                .collect(append(of(type.getSuperclass()).flatMap(Annotations::constants))).flatMap(cast(type));
    }

    public static Optional<? extends AnnotatedElement> ignore(@NotNull AnnotatedElement it) {
        return ignore(it, null);
    }

    public static Optional<? extends AnnotatedElement> ignore(@NotNull AnnotatedElement it, @Nullable Class<?> context) {
        var yield = findAnnotations(Ignore.class, it).findFirst();
        if (yield.isEmpty()) return Optional.empty();
        var anno  = yield.get();
        var types = anno.annotation.value();
        if (types.length == 0 || context == null) return Optional.of(it);
        return stream(types).filter(context::equals).findAny();
    }

    public static Stream<? extends Class<?>> related(@NotNull Class<?> it) {
        return findAnnotations(Related.class, it).map(Result::getAnnotation).map(Related::value).flatMap(Stream::of);
    }

    public static boolean ignoreInherit(AnnotatedElement target, Class<? extends Annotation> goal) {
        if (!(target instanceof Class) && !(target instanceof Member)) return true;
        Constraint.Type.noneOf(goal, "goal", Ignore.Inherit.class).run();

        if (target instanceof Constructor<?>) return true;
        return findAnnotations(Ignore.Inherit.class, target).anyMatch(result -> {
            var targets = result.annotation.value();
            return targets.length == 0 || asList(targets).contains(goal);
        });
    }

    public static <A extends Annotation> Stream<Result<A>> findAnnotations(final Class<A> type, final AnnotatedElement target) {
        //Constraint.Type.anyOf(target, "target", Class.class, Member.class).run();

        // @Ignore should inherit upwards indefinitely on anything but types; unless specified otherwise with @Ignore.Ancestors
        // @Alias should inherit only between ancestors of same type

        final var typeInherit = Wrap.of(type.getAnnotation(Inherit.class));
        final var decl        = ReflectionHelper.declaringClass(target);

        // do not scan system classes
        if (decl.getPackageName().startsWith("java")) return empty();

        // collect members
        return of(target).flatMap(member -> {
            var useAncestry = !Ignore.Inherit.class.isAssignableFrom(type) && !ignoreInherit(member, type);
            var inherit = typeInherit.orRef(() -> Wrap.of(member.getAnnotation(Inherit.class))).map(it -> {
                final var et = getElementType(member);
                return stream(it.rules()).filter(r -> r.on() == et).map(Inherit.Rule::strategy).findAny().orElseGet(it::value);
            }).orElse(Inherit.Type.Default);

            // expand with ancestors by local or annotations @Inheritance annotations
            var sources = of(member);
            if (useAncestry) switch (inherit) {
                case None:
                    break;
                case FromSupertype, FromBoth:
                    sources = sources.collect(append(findAncestor(member, type).stream()));
                    if (inherit != Inherit.Type.FromBoth) break;
                case FromParent:
                    sources = sources.collect(append(decl));
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + inherit);
            }
            else if (type.equals(Ignore.Inherit.class)) sources = sources.collect(append(decl));

            // get most relevant annotation
            return sources.flatMap(mem -> {
                if (mem == null) {
                    if (useAncestry) throw new AssertionError();
                    else return empty();
                }
                if (!mem.isAnnotationPresent(type)) return empty();
                return getRepeatableAnnotationsByType(mem, type).map(anno -> new Result<>(anno, member, mem, decl));
            });
        });
    }

    @SuppressWarnings("ConstantValue") // false positive
    public static Wrap<AnnotatedElement> findAncestor(AnnotatedElement target, Class<? extends Annotation> goal) {
        if (!(target instanceof Class) && !(target instanceof Member)) return Wrap.empty();

        Class<?> decl;
        if (target instanceof Class<?>) {
            decl = (Class<?>) target;
        } else if (target instanceof Member mem) {
            decl = mem.getDeclaringClass();
        } else throw new AssertionError("Invalid element: " + target);
        if (target instanceof Class<?> && decl.getPackageName().startsWith("java")) return Wrap.empty();
        try {
            if (target instanceof Member mem) {
                Member chk;
                if (mem instanceof DataStructure.Member dmem) if (dmem.getContext() instanceof Parameter) return Wrap.empty();
                else chk = (Member) dmem.getContext();
                else chk = mem;
                var pType = chk.getDeclaringClass().getSuperclass();
                if (pType == null) return Wrap.empty();
                // todo: this is inaccurate for different parameter overrides
                if (chk instanceof Method mtd) return Wrap.of(pType.getDeclaredMethod(mtd.getName(), mtd.getParameterTypes()));
                else if (chk instanceof Field fld) return Wrap.of(pType.getDeclaredField(fld.getName()));
                else if (chk instanceof Constructor<?> ctor) return Wrap.of(pType.getDeclaredConstructor(ctor.getParameterTypes()));
            } else if (target instanceof Class<?> cls) return Wrap.of(cls.getSuperclass());
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            return Wrap.empty();
        }
        throw new IllegalArgumentException("Invalid element: " + target);
    }

    public static String toString(Description... config) {
        if (config.length == 1) {
            var mode = config[0].mode();
            return stream(config).map(Description::value).flatMap(Arrays::stream).map(Translation::str).collect(Collectors.joining(switch (mode) {
                case Usage -> " ";
                case Lines -> "\n";
                case Steps -> "\n- ";
            }));
        } else return stream(config).sorted(Description.COMPARATOR).map(Annotations::toString).map(Translation::str).collect(Collectors.joining("\n\n"));
    }

    public static <T extends Member & AnnotatedElement> String toString(Expect expect, T member) {
        return "%s.%s does not return %s for Annotations.%s()".formatted(member.getDeclaringClass().getSimpleName(),
                member.getName(),
                expect.value(),
                expect.onTarget());
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private static <A extends Annotation> Stream<A> getRepeatableAnnotationsByType(AnnotatedElement member, Class<A> type) {
        if (!type.isAnnotationPresent(Repeatable.class)) return of(member.getAnnotation(type));
        var rep      = type.getAnnotation(Repeatable.class);
        var listType = rep.value();
        if (!member.isAnnotationPresent(listType)) return of(member.getAnnotation(type));
        var list = member.getAnnotation(listType);
        if (list == null) return empty();
        var mtd = listType.getMethod("value");
        A[] arr = (A[]) mtd.invoke(list);
        return stream(arr);
    }

    private static @NotNull ElementType getElementType(AnnotatedElement member) {
        member = unwrapStructMember(member);
        ElementType et;
        if (member instanceof Class<?>) et = ElementType.TYPE;
        else if (member instanceof Field) et = ElementType.FIELD;
        else if (member instanceof Method) et = ElementType.METHOD;
        else if (member instanceof Parameter) et = ElementType.PARAMETER;
        else if (member instanceof Constructor<?>) et = ElementType.CONSTRUCTOR;
        else if (member instanceof Package) et = ElementType.PACKAGE;
        else throw new RuntimeException("Unsupported member: " + member);
        return et;
    }

    private static AnnotatedElement unwrapStructMember(AnnotatedElement member) {
        if (member instanceof DataStructure.Member struct) return struct.getContext();
        return member;
    }

    @Value
    @AllArgsConstructor
    public static class Result<A extends Annotation> implements Annotation {
        @NotNull @Ignore A                annotation;
        @NotNull         AnnotatedElement context;
        @NotNull         AnnotatedElement annotated;
        @NotNull         Class<?>         declarator;

        @Override
        public Class<? extends Annotation> annotationType() {
            return annotation.annotationType();
        }
    }
}