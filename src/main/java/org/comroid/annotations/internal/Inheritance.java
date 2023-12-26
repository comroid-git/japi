package org.comroid.annotations.internal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.comroid.api.attr.BitmaskAttribute;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inheritance(Inheritance.Type.None)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Inheritance {
    Type value();

    @Getter
    @RequiredArgsConstructor
    enum Type implements BitmaskAttribute<Type> {
        None(0L),
        FromSupertype(1L),
        FromParent(2L),
        FromBoth(3L),
        Full(255L);

        private final Long value;
    }
}
