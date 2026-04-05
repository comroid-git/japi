package org.comroid.interaction.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.comroid.interaction.node.model.InteractionNode;
import org.comroid.interaction.registry.RegistrySource;

import java.util.List;

/**
 * a tree denoting all possible execution branches of a group-command (= callable) structure
 */
@Value
@Builder
public class InteractionTree {
    RegistrySource source;
    @Singular List<? extends InteractionNode> nodes;
}
