package org.comroid.interaction.node.model;

import org.comroid.interaction.model.InteractionContext;

public interface InvokableNode extends ContextManipulatingNode {
    Object invoke(InteractionContext context);
}
