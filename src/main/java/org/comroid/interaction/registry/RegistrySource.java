package org.comroid.interaction.registry;

import org.comroid.api.attr.Named;
import org.comroid.api.func.Specifiable;
import org.comroid.interaction.model.InteractionTree;

public interface RegistrySource extends Named, Specifiable<RegistrySource> {
    InteractionTree loadTree();
}
