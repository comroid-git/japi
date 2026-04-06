package org.comroid.interaction.annotation;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContextDefinition {
    /// context key
    String key();

    // expressions for values to set; each will be appended
    @Language(value = "JShellLanguage", prefix = "java.lang.Supplier x = () -> ", suffix = ";") String[] expr() default { };
}
