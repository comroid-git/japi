package org.comroid.api.net.luckperms.model.group;

import org.comroid.api.net.luckperms.model.dto.Metadata;
import org.comroid.api.net.luckperms.model.node.Node;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record GroupData(
        String name, @Nullable String displayName, int weight, Node[] nodes, @Nullable Metadata metadata
) {
    public String bestName() {
        return Objects.requireNonNullElse(displayName, name);
    }
}
