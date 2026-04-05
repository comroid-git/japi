package org.comroid.interaction.component.error;

import org.comroid.interaction.model.InteractionContext;

public interface ErrorHandler {
    State handle(InteractionContext context, Throwable error);

    enum State {UNCHANGED, RECOVERED}
}
