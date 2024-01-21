package org.comroid.annotations;

import org.comroid.annotations.internal.Inherit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Inherit(Inherit.Type.FromParent)
public @interface Alias {
    String[] value() default {};
}
