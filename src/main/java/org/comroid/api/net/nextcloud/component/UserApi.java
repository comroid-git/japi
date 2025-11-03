package org.comroid.api.net.nextcloud.component;

import lombok.Value;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.net.nextcloud.model.OcsApiComponent;
import org.comroid.api.net.nextcloud.model.OcsApiCore;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Value
public class UserApi extends OcsApiComponent {
    public UserApi(OcsApiCore ocsApi) {
        super(ocsApi);
    }

    public CompletableFuture<Collection<String>> getUsers() {
        return getOcsApi().get("/ocs/v1.php/cloud/users")
                .thenApply(data -> data.get("ocs")
                        .get("data")
                        .get("users")
                        .asArray()
                        .stream()
                        .map(DataNode::asValue)
                        .map(DataNode::asString)
                        .toList());
    }
}
