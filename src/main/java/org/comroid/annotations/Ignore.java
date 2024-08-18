package org.comroid.annotations;

import org.comroid.annotations.internal.Inherit;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a member to be ignored by parsing
 */
@Inherit(value = Inherit.Type.FromBoth, rules = @Inherit.Rule(on = ElementType.TYPE, strategy = Inherit.Type.None))
@Retention(RetentionPolicy.RUNTIME)
public @interface Ignore {
    /**
     * @return what utility should ignore the member
     */
    Class<?>[] value() default { };

    /**
     * Marks a member so that the {@link Ignore} annotation is ignored on its ancestors
     * <p>
     * Useful for explicitly including Ignored members
     */
    @org.comroid.annotations.internal.Inherit(org.comroid.annotations.internal.Inherit.Type.FromParent)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Inherit {
        /**
         * @return on which annotation processing to ignore the ancestors
         */
        Class<? extends Annotation>[] value() default { };
    }
}
