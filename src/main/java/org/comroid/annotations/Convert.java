package org.comroid.annotations;

import org.comroid.annotations.internal.Inherit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Inherit(Inherit.Type.FromSupertype)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface Convert {
    String identifyVia() default "";
}
