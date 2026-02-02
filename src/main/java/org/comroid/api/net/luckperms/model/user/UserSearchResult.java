package org.comroid.api.net.luckperms.model.user;

import org.comroid.api.net.luckperms.model.node.Node;

import java.util.UUID;

public record UserSearchResult(UUID uniqueId, Node[] results) {
}
