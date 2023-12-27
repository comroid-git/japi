package org.comroid.annotations;

import org.comroid.annotations.internal.Inheritance;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a member to be ignored by parsing
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Inheritance(Inheritance.Type.FromBoth)
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
    @Inheritance(Inheritance.Type.FromParent)
    @interface Ancestor {
        /**
         * @return on which annotation processing to ignore the ancestors
         */
        Class<? extends Annotation>[] value() default {};
    }
}
