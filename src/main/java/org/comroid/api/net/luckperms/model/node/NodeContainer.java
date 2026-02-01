package org.comroid.api.net.luckperms.model.node;

import org.comroid.api.net.luckperms.model.dto.Metadata;
import org.comroid.api.net.luckperms.model.dto.PermissionCheckResult;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface NodeContainer<ID> {
    CompletableFuture<Collection<Node>> getNodes(ID id);

    CompletableFuture<Metadata> getMetadata(ID id);

    CompletableFuture<PermissionCheckResult> checkPermission(ID id, CharSequence permission);
}
