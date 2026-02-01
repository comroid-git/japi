package org.comroid.api.net.luckperms.model;

import org.comroid.api.data.seri.DataNode;
import org.comroid.api.model.Authentication;
import org.comroid.api.net.REST;
import org.comroid.api.tree.Component;

import java.util.concurrent.CompletableFuture;

public interface LuckPermsApiCore extends Component {
    String getBaseUrl();

    REST getRest();

    Authentication getCredentials();

    REST.Request request(REST.Method method, String path);

    CompletableFuture<DataNode> get(String path);
}
