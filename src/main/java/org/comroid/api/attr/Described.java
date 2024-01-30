package org.comroid.api.attr;

import org.comroid.annotations.AnnotatedTarget;
import org.comroid.annotations.internal.Annotations;

public interface Described extends AnnotatedTarget.Extension {
    default String getDescription() {
        return element()
                .map(Annotations::descriptionText)
                .orElse("No description");
    }
}
