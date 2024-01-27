package org.comroid.annotations.internal;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Repeatable(Expects.class)
@Inherit(Inherit.Type.None)
@Retention(RetentionPolicy.RUNTIME)
public @interface Expect {
    @Language("JShellLanguage") String value();

    String onTarget();

    Class<?> type() default void.class;
}
