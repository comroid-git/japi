package org.comroid.interaction.registry;

import org.comroid.api.attr.Named;
import org.comroid.interaction.model.InteractionTree;

public interface RegistrySource extends Named {
    InteractionTree loadTree();
}
