package org.comroid.annotations.codegen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @see DelegateTarget
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface DelegateSource {
    /**
     * If defined, declares the identifier used to recognize the accumulated value.
     * Otherwise, the annotated member name is used.
     *
     * @return identifier of the output value
     */
    String value() default "§";
}
