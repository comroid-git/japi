package org.comroid.annotations;

import lombok.experimental.UtilityClass;
import org.comroid.api.SupplierX;
import org.comroid.util.StackTraceUtils;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;

@UtilityClass
@ApiStatus.Internal
public class Annotations {
    public static final Class<?>[] SystemFilters = new Class<?>[]{Object.class,Class.class};

    public boolean ignore(AnnotatedElement it, Class<?> requester) {
        var yield = findAnnotation(Ignore.class, it);
        if (yield.isNull())
            return false;
        var anno = yield.assertion();
        var types = anno.value();
        if (types.length == 0)
            return true;
        return Arrays.asList(types).contains(requester);
    }

    public <A extends Annotation> SupplierX<A> findAnnotation(final Class<A> type, final AnnotatedElement in) {
        return SupplierX.of(in.getAnnotation(type))
                .orRef(() -> SupplierX.of(findParent(in))
                        .flatMap(parent -> findAnnotation(type, parent)));
    }

    private static AnnotatedElement findParent(AnnotatedElement in) {
        if (in instanceof Class<?> && Arrays.asList(SystemFilters).contains(in))
            return in;
        try {
            if (in instanceof Member) {
                var pType = ((Member) in).getDeclaringClass().getSuperclass();
                // todo: this is inaccurate for different parameter overrides
                if (in instanceof Method)
                    return pType.getDeclaredMethod(((Method) in).getName(), ((Method) in).getParameterTypes());
                else if (in instanceof Field)
                    return pType.getDeclaredField(((Field) in).getName());
                else if (in instanceof Constructor<?>)
                    return pType.getDeclaredConstructor(((Constructor<?>) in).getParameterTypes());
            } else if (in instanceof Class)
                return ((Class<?>) in).getSuperclass();
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            return null;
        }
        throw new IllegalArgumentException("Invalid element: " + in);
    }
}