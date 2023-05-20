package org.comroid.annotations;

import org.comroid.api.Context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the Member of a class that can be used to obtain a singleton-like instance of the class.
 * Result of the Member should always be equal, tho this is not checked.
 * The annotated member must be {@code static}, and may not have any parameters other than {@link Context}.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface Instance {
}
