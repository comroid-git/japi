package org.comroid.annotations.internal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Inherit(Inherit.Type.None)
@Retention(RetentionPolicy.RUNTIME)
public @interface Expects {
    Expect[] value();
}
