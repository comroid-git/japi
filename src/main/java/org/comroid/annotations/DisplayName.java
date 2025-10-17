package org.comroid.annotations;

import org.comroid.annotations.internal.Annotations;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

public @interface DisplayName {
    String value();

    class $ {
        public static Optional<String> of(@NotNull AnnotatedElement context) {
            return Annotations.findAnnotations(DisplayName.class, context)
                    .findAny()
                    .map(Annotations.Result::getAnnotation)
                    .map(DisplayName::value);
        }
    }
}
