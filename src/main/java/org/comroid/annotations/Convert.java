package org.comroid.annotations;

import org.comroid.annotations.internal.Inherit;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Inherit(Inherit.Type.FromSupertype)
@Target({ElementType.METHOD,ElementType.CONSTRUCTOR})
public @interface Convert {
    String identifyVia() default "";
}
