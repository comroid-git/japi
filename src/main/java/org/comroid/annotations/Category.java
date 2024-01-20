package org.comroid.annotations;

import org.comroid.annotations.internal.Inherit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Inherit(Inherit.Type.FromBoth)
public @interface Category {
    String value() default "";

    Description[] desc() default {};
}
