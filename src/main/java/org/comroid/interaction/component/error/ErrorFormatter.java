package org.comroid.interaction.component.error;

import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.model.Response;

import java.util.Optional;

public interface ErrorFormatter {
    Optional<Response> format(InteractionContext context, Throwable error);
}
