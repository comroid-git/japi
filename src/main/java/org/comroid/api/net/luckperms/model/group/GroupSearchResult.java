package org.comroid.api.net.luckperms.model.group;

import org.comroid.api.net.luckperms.model.node.Node;

public record GroupSearchResult(String name, Node[] results) {
}
