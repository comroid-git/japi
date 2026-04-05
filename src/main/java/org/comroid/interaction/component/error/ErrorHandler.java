package org.comroid.interaction.component.error;

import org.comroid.interaction.model.InteractionContext;
import org.jspecify.annotations.Nullable;

public interface ErrorHandler {
    @Nullable Object handle(InteractionContext context, Throwable error);
}
