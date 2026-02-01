package org.comroid.api.net.luckperms.model.user;

import org.comroid.api.net.luckperms.model.node.Node;

import java.util.Collection;
import java.util.UUID;

public record UserSearchResult(UUID uniqueId, Collection<Node> results) {
}
