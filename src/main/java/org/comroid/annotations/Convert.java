package org.comroid.annotations;

import org.comroid.annotations.internal.Inheritance;

import java.lang.annotation.*;

@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inheritance(Inheritance.Type.FromSupertype)
public @interface Convert {
    String identifyVia() default "";
}
