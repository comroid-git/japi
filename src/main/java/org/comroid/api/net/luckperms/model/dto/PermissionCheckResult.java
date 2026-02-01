package org.comroid.api.net.luckperms.model.dto;

import org.comroid.api.net.luckperms.model.node.Node;

public record PermissionCheckResult(boolean result, Node node) {
}
