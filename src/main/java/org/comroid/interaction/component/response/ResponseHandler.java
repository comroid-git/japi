package org.comroid.interaction.component.response;

import org.comroid.interaction.model.InteractionContext;

import java.util.concurrent.CompletableFuture;

public interface ResponseHandler<T> {
    Class<T> getResponseType();

    /// an implementation may choose to override this stub to provide additional context data for a deferred response on async interactions
    default CompletableFuture<?> deferResponse(InteractionContext context) {
        return CompletableFuture.completedFuture(null);
    }

    void sendResponse(InteractionContext context, T response);
}
