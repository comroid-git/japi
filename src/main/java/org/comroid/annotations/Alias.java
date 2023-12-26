package org.comroid.annotations;

import org.comroid.annotations.internal.Inheritance;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Inheritance(Inheritance.Type.FromSupertype)
public @interface Alias {
    String[] value() default {};
}
