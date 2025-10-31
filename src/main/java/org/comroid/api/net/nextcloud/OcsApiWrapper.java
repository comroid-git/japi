package org.comroid.api.net.nextcloud;

import lombok.Builder;
import lombok.Value;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.model.Authentication;
import org.comroid.api.net.REST;
import org.comroid.api.net.nextcloud.component.FilesApi;
import org.comroid.api.net.nextcloud.component.TablesApi;
import org.comroid.api.net.nextcloud.component.UserApi;
import org.comroid.api.net.nextcloud.model.OcsApiCore;
import org.comroid.api.tree.Component;

import java.util.concurrent.CompletableFuture;

@Value
@Builder
public class OcsApiWrapper extends Component.Base implements OcsApiCore {
    @lombok.Builder.Default REST rest = REST.Default;
    String         baseUrl;
    Authentication credentials;

    {
        addChildren(new UserApi(this), new FilesApi(this), new TablesApi(this));
    }

    @Override
    public REST.Request request(REST.Method method, String path) {
        return rest.new Request(method, baseUrl + path)
                .addHeader("Accept", "application/json")
                .addHeader("OCS-APIRequest", "true")
                .addHeader("Authorization", credentials.toHttpBasicHeader().getValue());
    }

    @Override
    public CompletableFuture<DataNode> get(String path) {
        return request(REST.Method.GET, path)
                .execute()
                .thenApply(REST.Response::validate2xxOK)
                .thenApply(REST.Response::getBody);
    }
}
