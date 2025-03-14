package org.comroid.api.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** indicates that a value should not be displayed in editors */
@Retention(RetentionPolicy.RUNTIME)
public @interface Adapt {
    /** determines a class that should be {@linkplain org.comroid.api.java.JITAssistant#prepare(Class[]) loaded} first in order to initialize it to cache */
    Class<?>[] value() default { };
}
