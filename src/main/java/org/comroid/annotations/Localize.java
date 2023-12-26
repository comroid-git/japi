package org.comroid.annotations;

import org.comroid.annotations.internal.Inheritance;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Inheritance(Inheritance.Type.None)
public @interface Localize {
    String value() default "";

    @Retention(RetentionPolicy.RUNTIME)
    @Inheritance(Inheritance.Type.None)
    @interface Imperative {}
}
