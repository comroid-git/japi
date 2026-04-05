package org.comroid.interaction.node;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.node.model.ContextManipulatingNode;
import org.comroid.interaction.node.model.InteractionNode;
import org.comroid.interaction.node.model.ParentNode;
import org.comroid.interaction.registry.RegistrySource;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
public class GroupNode extends InteractionNode implements ParentNode, ContextManipulatingNode {
    Class<?>                        type;
    List<? extends InteractionNode> children;

    public GroupNode(@NonNull RegistrySource source, Interaction.Resolved interaction, Class<?> type, List<? extends InteractionNode> children) {
        super(source, interaction);

        this.type     = type;
        this.children = children;
    }

    public Interaction.@Nullable Element toInteractionElement() {
        return new Interaction.Element(interaction, type);
    }

    @Override
    public Object invoke(InteractionContext context) {
        return children.stream().filter(node -> "$".equals(node.getName())).findAny().orElseThrow().invoke(context);
    }
}
