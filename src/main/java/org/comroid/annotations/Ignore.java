package org.comroid.annotations;

import java.lang.annotation.Annotation;
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
    Class<?>[] value() default {};

    /**
     * Marks a member so that the {@link Ignore} annotation is ignored on its ancestors
     * <p>
     * Useful for explicitly including Ignored members
     */
    @Retention(RetentionPolicy.RUNTIME)
    @interface Ancestor {
        /**
         * @return on which annotation processing to ignore the ancestors
         */
        Class<? extends Annotation>[] value() default {};
    }
}
