package org.comroid.commands.impl.minecraft;

import org.comroid.commands.impl.CommandUsage;
import org.comroid.commands.model.CommandResponseHandler;

import java.util.Optional;

public interface MinecraftResponseHandler extends CommandResponseHandler {
    @Override
    default Optional<String> handleThrowable(CommandUsage usage, Throwable throwable) {
        return Optional.of("§c" + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
    }
}
