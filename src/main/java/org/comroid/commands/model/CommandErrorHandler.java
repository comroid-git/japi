package org.comroid.commands.model;

import org.comroid.commands.impl.CommandUsage;

import java.util.Optional;

public interface CommandErrorHandler {
    Optional<String> handleThrowable(CommandUsage usage, Throwable throwable);
}
