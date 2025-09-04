package org.comroid.commands.impl.minecraft;

import org.comroid.commands.model.CommandResponseHandler;

public interface MinecraftResponseHandler extends CommandResponseHandler {
    @Override
    default String handleThrowable(Throwable throwable) {
        return "§c" + throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
    }
}
