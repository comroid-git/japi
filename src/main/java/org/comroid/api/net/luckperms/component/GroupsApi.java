package org.comroid.api.net.luckperms.component;

import lombok.Value;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.net.luckperms.model.LuckPermsApiComponent;
import org.comroid.api.net.luckperms.model.LuckPermsApiCore;
import org.comroid.api.net.luckperms.model.ObjectRepository;
import org.comroid.api.net.luckperms.model.dto.Metadata;
import org.comroid.api.net.luckperms.model.dto.PermissionCheckResult;
import org.comroid.api.net.luckperms.model.group.GroupData;
import org.comroid.api.net.luckperms.model.group.GroupSearchResult;
import org.comroid.api.net.luckperms.model.node.Node;
import org.comroid.api.net.luckperms.model.node.NodeContainer;
import org.comroid.api.net.luckperms.model.node.NodeType;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Value
public class GroupsApi extends LuckPermsApiComponent
        implements NodeContainer<String>, ObjectRepository<String, GroupData> {
    public GroupsApi(LuckPermsApiCore lpApi) {
        super(lpApi);
    }

    @Override
    public CompletableFuture<Collection<String>> getIDs() {
        return getLpApi().get("/group").thenApply(data -> data.asArray().stream().map(DataNode::asString).toList());
    }

    @Override
    public CompletableFuture<GroupData> get(String name) {
        return getLpApi().get("/group/" + name)
                .thenApply(data -> data.as(GroupData.class).orElseThrow(() -> new RuntimeException()));
    }

    @lombok.Builder(builderMethodName = "search", buildMethodName = "execute", builderClassName = "SearchQuery")
    public CompletableFuture<Collection<GroupSearchResult>> searchQuery(
            @Nullable String nodeKey, @Nullable String nodeKeyStartsWith, @Nullable String metaKey,
            @Nullable NodeType nodeType, @Nullable String group
    ) {
        var separator = '?';
        var path      = "/group/search";

        if (nodeKey != null) {
            path += separator + "key=" + nodeKey;
            separator = '&';
        }
        if (nodeKeyStartsWith != null) {
            path += separator + "keyStartsWith=" + nodeKeyStartsWith;
            separator = '&';
        }
        if (metaKey != null) {
            path += separator + "metaKey=" + metaKey;
            separator = '&';
        }
        if (nodeType != null) {
            path += separator + "type=" + nodeType.name();
            separator = '&';
        }
        if (group != null) {
            path += separator + "group=" + group;
            separator = 0;
        }

        if (separator == '?') throw new IllegalArgumentException("At least one parameter must be provided");
        return getLpApi().get(path)
                .thenApply(data -> data.asArray()
                        .stream()
                        .map(node -> node.as(GroupSearchResult.class).assertion())
                        .toList());
    }

    @Override
    public CompletableFuture<Collection<Node>> getNodes(String name) {
        return getLpApi().get("/group/" + name + "/nodes")
                .thenApply(data -> data.asArray().stream().map(node -> node.as(Node.class).assertion()).toList());
    }

    @Override
    public CompletableFuture<Metadata> getMetadata(String name) {
        return getLpApi().get("/group/" + name + "/meta").thenApply(data -> data.as(Metadata.class).assertion());
    }

    @Override
    public CompletableFuture<PermissionCheckResult> checkPermission(String name, CharSequence permission) {
        return getLpApi().get("/group/" + name + "/permission-check?permission=" + permission)
                .thenApply(data -> data.as(PermissionCheckResult.class).assertion());
    }
}
