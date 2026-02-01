package org.comroid.api.net.luckperms.model.group;

import org.comroid.api.net.luckperms.model.node.Node;

import java.util.Collection;

public record GroupSearchResult(String name, Collection<Node> results) {
}
