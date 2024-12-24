package org.comroid.annotations.codegen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @see DelegateSource
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER })
public @interface DelegateTarget {
    /**
     * If defined, declares the identifier used to recognize the accumulated value.
     * Otherwise, the annotated member name is used.
     *
     * @return identifier of the input value
     */
    String value() default "§";

    /**
     * If defined, declares the type of the parameters to generate delegate methods for.
     * Otherwise, this value is ignored.
     *
     * @return type of parameter to delegate
     * @see #member()
     */
    Class<?> type() default void.class;

    /**
     * If defined, declares the member of the accumulated value.
     * Otherwise, the value itself is returned.
     *
     * @return member to obtain value from
     * @see #type()
     */
    String member() default "§";
}
