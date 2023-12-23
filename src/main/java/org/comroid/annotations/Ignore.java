package org.comroid.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a member to be ignored by parsing
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Ignore {
    /**
     * @return what utility should ignore the member
     */
    Class<?>[] value() default void.class;
}
