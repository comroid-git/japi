package org.comroid.api.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** indicates that a value should not be displayed in editors */
@Retention(RetentionPolicy.RUNTIME)
public @interface Adapt {
    /** determines a set of type adapters that are used for the annotated target */
    Class<?>[] value() default { };
}
