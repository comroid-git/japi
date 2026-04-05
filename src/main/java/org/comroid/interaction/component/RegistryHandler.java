package org.comroid.interaction.component;

import org.comroid.interaction.model.InteractionTree;

import java.util.Collection;

public interface RegistryHandler {
    Collection<InteractionTree> getRegistered();

    void register(InteractionTree tree);
}
