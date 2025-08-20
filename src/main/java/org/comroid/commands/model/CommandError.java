package org.comroid.commands.model;

import lombok.Getter;
import org.comroid.api.text.Translation;
import org.comroid.commands.impl.CommandUsage;
import org.jetbrains.annotations.Nullable;

@Getter
public class CommandError extends RuntimeException {
    private final @Nullable Object       response;
    private final @Nullable CommandUsage command;

    public CommandError(String message) {
        this(message, null);
    }

    public CommandError(String message, @Nullable Throwable cause) {
        this(message, cause, null);
    }

    public CommandError(@Nullable String message, @Nullable Throwable cause, @Nullable Object response) {
        this(message, cause, response, null);
    }

    @lombok.Builder
    public CommandError(
            @Nullable String message, @Nullable Throwable cause, @Nullable Object response,
            @Nullable CommandUsage command
    ) {
        super(Translation.str(message), cause);
        this.response = response;
        this.command  = command;
    }

    public CommandError(Object response) {
        this(response, null);
    }

    public CommandError(Object response, @Nullable Throwable cause) {
        this(null, cause, response);
    }
}
