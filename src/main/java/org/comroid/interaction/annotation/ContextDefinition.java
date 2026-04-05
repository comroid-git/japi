package org.comroid.interaction.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContextDefinition {
    /// context key
    String value();

    // expressions for values to set; each will be appended
    String[] expression() default { };
}
