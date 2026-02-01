package org.comroid.api.net.luckperms.model.user;

import org.comroid.api.net.luckperms.model.dto.Metadata;
import org.comroid.api.net.luckperms.model.node.Node;

import java.util.Collection;
import java.util.UUID;

public record UserData(
        UUID uniqueId, String username, Collection<String> parentGroups, Collection<Node> nodes, Metadata metadata
) {}
