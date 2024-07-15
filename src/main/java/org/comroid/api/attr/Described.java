package org.comroid.api.attr;

import org.comroid.annotations.AnnotatedTarget;
import org.comroid.annotations.internal.Annotations;

import static java.util.function.Predicate.*;

public interface Described extends AnnotatedTarget.Extension {
    default String getDescription() {
        return element()
                .map(Annotations::descriptionText)
                .filter(not(String::isBlank))
                .orElse("No description");
    }
}
