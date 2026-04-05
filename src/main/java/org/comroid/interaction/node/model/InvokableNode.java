package org.comroid.interaction.node.model;

import org.comroid.api.attr.Described;
import org.comroid.api.attr.Named;
import org.comroid.interaction.model.InteractionContext;

public interface InvokableNode extends ContextManipulatingNode, Named, Described {
    Object invoke(InteractionContext context);
}
