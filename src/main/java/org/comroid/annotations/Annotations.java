package org.comroid.annotations;

import lombok.experimental.UtilityClass;
import org.comroid.api.SupplierX;
import org.comroid.util.Streams;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings({"DuplicatedCode","BooleanMethodIsAlwaysInverted"})
@UtilityClass
@ApiStatus.Internal
public class Annotations {
    public boolean ignore(AnnotatedElement it, Class<?> context) {
        var yield = findAnnotation(Ignore.class, it);
        if (yield.isNull())
            return false;
        var anno = yield.assertion();
        var types = anno.value();
        if (types.length == 0)
            return true;
        return Arrays.asList(types).contains(context);
    }

    public <A extends Annotation> SupplierX<A> findAnnotation(final Class<A> type, final AnnotatedElement in) {
        Class<?> decl;
        if (in instanceof Class<?>) {
            decl = (Class<?>) in;
        } else if (in instanceof Member) {
            decl = ((Member) in).getDeclaringClass();
        } else throw new IllegalArgumentException("Invalid element: " + in);
        if (decl.getPackageName().startsWith("java"))
            return SupplierX.empty();

        // first, try get annotation from type
        return SupplierX.of(in.getAnnotation(type))
                // otherwise, recurse into a couple of related members
                .orOpt(() -> (in instanceof Class<?>
                        // for class, try supertypes
                        ? Stream.of(decl).flatMap(c -> Stream.concat(
                        Stream.of(c.getSuperclass()),
                        Arrays.stream(c.getInterfaces())))
                        // for members, try declaring class first
                        : Stream.of(decl)
                        // if applicable, try return types next
                        .collect(Streams.append(Stream.of(in).map(x -> {
                            if (x instanceof Field)
                                return ((Field) x).getType();
                            else if (x instanceof Method)
                                return ((Method) x).getReturnType();
                            else return null;
                        })))
                        // otherwise try ancestors
                        .collect(Streams.append(Stream.of(in).flatMap(x -> findAncestor(x).stream()))))
                        .filter(Objects::nonNull)
                        .distinct()
                        // recurse
                        .flatMap(x -> findAnnotation(type, x).stream())
                        .findFirst());
    }

    @SuppressWarnings("ConstantValue") // false positive
    private static SupplierX<AnnotatedElement> findAncestor(AnnotatedElement of) {
        Class<?> decl;
        if (of instanceof Class<?>) {
            decl = (Class<?>) of;
        } else if (of instanceof Member) {
            decl = ((Member) of).getDeclaringClass();
        } else throw new IllegalArgumentException("Invalid element: " + of);
        if (decl.getPackageName().startsWith("java"))
            return SupplierX.empty();

        try {
            if (of instanceof Member) {
                var pType = ((Member) of).getDeclaringClass().getSuperclass();
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