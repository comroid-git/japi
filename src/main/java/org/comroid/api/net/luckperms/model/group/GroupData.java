package org.comroid.api.net.luckperms.model.group;

import org.comroid.api.net.luckperms.model.node.Node;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public record GroupData(String name, @Nullable String displayName, int weight, Collection<Node> nodes) {
}
