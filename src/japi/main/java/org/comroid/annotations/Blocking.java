package org.comroid.annotations;

import org.comroid.annotations.internal.Inherit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Inherit(Inherit.Type.None)
@Retention(RetentionPolicy.RUNTIME)
public @interface Blocking {
}
