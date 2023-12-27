package org.comroid.annotations;

import org.comroid.annotations.internal.Inherit;

import java.lang.annotation.*;

@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherit(Inherit.Type.FromSupertype)
public @interface Convert {
    String identifyVia() default "";
}
