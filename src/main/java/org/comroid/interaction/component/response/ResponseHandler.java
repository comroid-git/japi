package org.comroid.interaction.component.response;

import org.comroid.interaction.model.InteractionContext;

public interface ResponseHandler<T> {
    Class<T> getResponseType();

    /// an implementation may choose to override this stub to provide additional context data for a deferred response on async interactions
    default void deferResponse(InteractionContext context) {
    }

    void sendResponse(InteractionContext context, T response);
}
