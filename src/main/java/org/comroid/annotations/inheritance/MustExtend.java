package org.comroid.annotations.inheritance;

import org.comroid.annotations.internal.Inheritance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Inheritance(Inheritance.Type.None)
public @interface MustExtend {
    Class<?> value();
}
