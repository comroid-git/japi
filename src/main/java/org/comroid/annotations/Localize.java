package org.comroid.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Localize {
    String value() default "";

    @Retention(RetentionPolicy.CLASS)
    @interface Imperative {}
}
