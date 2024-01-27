package org.comroid.annotations.inheritance;

import org.comroid.annotations.internal.Inherit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Inherit(Inherit.Type.None)
public @interface MustExtend {
    Class<?> value();
}
