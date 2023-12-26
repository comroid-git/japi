package org.comroid.annotations;

import org.comroid.annotations.internal.Inheritance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Inheritance(Inheritance.Type.None)
@Retention(RetentionPolicy.RUNTIME)
public @interface Blocking {
}
