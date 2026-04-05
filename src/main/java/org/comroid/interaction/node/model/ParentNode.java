package org.comroid.interaction.node.model;

import java.util.List;

public interface ParentNode {
    List<? extends InteractionNode> getChildren();
}
