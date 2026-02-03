package org.comroid.api.net.luckperms.model.user;

import org.comroid.api.net.luckperms.model.dto.Metadata;
import org.comroid.api.net.luckperms.model.node.Node;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record UserData(
        UUID uniqueId, String username, String[] parentGroups, Node[] nodes, @Nullable Metadata metadata
) {}
