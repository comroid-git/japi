package org.comroid.commands;

import org.comroid.api.text.StringMode;
import org.comroid.commands.autofill.IAutoFillProvider;
import org.comroid.commands.model.CommandPrivacyLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@SuppressWarnings("unused")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Command {
    String EmptyAttribute = "@@@";

    String value() default EmptyAttribute;

    String usage() default EmptyAttribute;

    String permission() default EmptyAttribute;

    CommandPrivacyLevel privacy() default CommandPrivacyLevel.EPHEMERAL;

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Arg {
        String value() default EmptyAttribute;

        int index() default -1;

        String[] autoFill() default { };

        Class<? extends IAutoFillProvider>[] autoFillProvider() default { };

        boolean required() default true;

        StringMode stringMode() default StringMode.NORMAL;
    }
}
