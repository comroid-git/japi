package org.comroid.annotations;

import org.comroid.annotations.internal.Annotations;
import org.comroid.annotations.internal.Inherit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;
import java.util.Comparator;

@Retention(RetentionPolicy.RUNTIME)
@Inherit(Inherit.Type.FromSupertype)
public @interface Order {
    Comparator<AnnotatedElement> COMPARATOR = Comparator.comparingInt(it ->
            Annotations.findAnnotations(Order.class, it)
                    .findAny()
                    .map(Annotations.Result::getAnnotation)
                    .map(Order::value)
                    .orElse(0));

    int value();
}
