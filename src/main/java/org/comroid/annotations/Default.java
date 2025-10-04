package org.comroid.annotations;

import org.comroid.annotations.internal.Annotations;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
public @interface Default {
    @ApiStatus.Experimental
    @Language("JShellLanguage")
    String value() default "null";

    interface Extension extends AnnotatedTarget.Extension {
        default @Nullable Object defaultValue() {
            return element().ifPresentMap(Annotations::defaultValue);
        }
    }
}
