package org.comroid.interaction.node.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.registry.RegistrySource;
import org.jspecify.annotations.NonNull;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
public abstract class InteractionNode extends Node implements InvokableNode, ContextManipulatingNode {
    Interaction.Resolved interaction;

    public InteractionNode(@NonNull RegistrySource source, Interaction.Resolved interaction) {
        super(source);

        this.interaction = interaction;
    }
}
