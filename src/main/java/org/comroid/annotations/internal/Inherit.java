package org.comroid.annotations.internal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.comroid.api.attr.BitmaskAttribute;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Inherit(Inherit.Type.None)
@Retention(RetentionPolicy.RUNTIME)
public @interface Inherit {
    Type value() default Type.FromBoth;

    Rule[] rules() default {};

    @Getter
    @RequiredArgsConstructor
    enum Type implements BitmaskAttribute<Type> {
        Rules(-1L),
        None(0L),
        FromSupertype(1L),
        FromParent(2L),
        FromBoth(3L);

        public static final Type Default = FromBoth;

        private final Long value;
    }

    @interface Rule {
        ElementType on();

        Type strategy();
    }
}
