package org.comroid.annotations;

import org.comroid.annotations.internal.Inherit;
import org.comroid.api.func.ext.Context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the Member of a class that can be used to obtain a singleton-like instance of the class.
 * Result of the Member should always be equal, tho this is not checked.
 * The annotated member must be {@code static}, and may not have any parameters other than {@link Context}.
 */
@Inherit(Inherit.Type.None)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR })
public @interface Instance {
}
