package org.comroid.api.net.luckperms.model.node;

import org.comroid.api.net.luckperms.model.dto.ContextEntry;

import java.util.Collection;

public record Node(String key, NodeType type, boolean value, Collection<ContextEntry> context) {
    public String getKeyValue() {
        var i = key.lastIndexOf('.');
        return key.substring(i + 1);
    }
}
