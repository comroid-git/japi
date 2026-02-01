package org.comroid.api.net.luckperms.component;

import lombok.Value;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.net.luckperms.model.LuckPermsApiComponent;
import org.comroid.api.net.luckperms.model.LuckPermsApiCore;
import org.comroid.api.net.luckperms.model.ObjectRepository;
import org.comroid.api.net.luckperms.model.dto.Metadata;
import org.comroid.api.net.luckperms.model.dto.PermissionCheckResult;
import org.comroid.api.net.luckperms.model.node.Node;
import org.comroid.api.net.luckperms.model.node.NodeContainer;
import org.comroid.api.net.luckperms.model.node.NodeType;
import org.comroid.api.net.luckperms.model.user.PlayerData;
import org.comroid.api.net.luckperms.model.user.UserData;
import org.comroid.api.net.luckperms.model.user.UserSearchResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Value
@SuppressWarnings("unused")
public class UserApi extends LuckPermsApiComponent implements NodeContainer<UUID>, ObjectRepository<UUID, UserData> {
    public UserApi(LuckPermsApiCore lpApi) {
        super(lpApi);
    }

    @Override
    public CompletableFuture<Collection<UUID>> getIDs() {
        return getLpApi().get("/user")
                .thenApply(data -> data.asArray()
                        .stream()
                        .map(DataNode::asString)
                        .filter(Objects::nonNull)
                        .map(UUID::fromString)
                        .toList());
    }

    @Override
    public CompletableFuture<UserData> get(UUID uniqueId) {
        return getLpApi().get("/user/" + uniqueId).thenApply(data -> data.as(UserData.class).assertion());
    }

    @lombok.Builder(builderMethodName = "lookup", buildMethodName = "execute", builderClassName = "LookupQuery")
    public CompletableFuture<PlayerData> lookupQuery(@Nullable String username, @Nullable UUID uniqueId) {
        var path = "/user/lookup";
        if (username != null) path += "?username=" + username;
        else if (uniqueId != null) path += "?uniqueId=" + uniqueId;
        else throw new IllegalArgumentException("One of ['username', 'uniqueId'] must be provided");
        return getLpApi().get(path).thenApply(data -> data.as(PlayerData.class).assertion());
    }

    @lombok.Builder(builderMethodName = "search", buildMethodName = "execute", builderClassName = "SearchQuery")
    public CompletableFuture<Collection<UserSearchResult>> searchQuery(
            @Nullable String nodeKey, @Nullable String nodeKeyStartsWith, @Nullable String metaKey,
            @Nullable NodeType nodeType, @Nullable String group
    ) {
        var separator = '?';
        var path      = "/user/search";

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
                        .map(node -> node.as(UserSearchResult.class).assertion())
                        .toList());
    }

    @Override
    public CompletableFuture<Collection<Node>> getNodes(UUID uniqueId) {
        return getLpApi().get("/user/" + uniqueId + "/nodes")
                .thenApply(data -> data.asArray().stream().map(node -> node.as(Node.class).assertion())

                        .toList());
    }

    @Override
    public CompletableFuture<Metadata> getMetadata(UUID uniqueId) {
        return getLpApi().get("/user/" + uniqueId + "/meta").thenApply(data -> data.as(Metadata.class).assertion());
    }

    @Override
    public CompletableFuture<PermissionCheckResult> checkPermission(UUID uniqueId, CharSequence permission) {
        return getLpApi().get("/user/" + uniqueId + "/permission-check?permission=" + permission)
                .thenApply(data -> data.as(PermissionCheckResult.class).assertion());
    }
}
