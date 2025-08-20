package org.comroid.commands.model;

import java.util.Set;

public enum CommandCapability {
    NAMED_ARGS;

    @FunctionalInterface
    public interface Provider {
        Set<CommandCapability> getCapabilities();

        default boolean hasCapability(CommandCapability capability) {
            return getCapabilities().stream().anyMatch(capability::equals);
        }
    }
}
