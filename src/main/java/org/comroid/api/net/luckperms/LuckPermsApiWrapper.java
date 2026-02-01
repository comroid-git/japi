package org.comroid.api.net.luckperms;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.model.Authentication;
import org.comroid.api.net.REST;
import org.comroid.api.net.luckperms.component.GroupsApi;
import org.comroid.api.net.luckperms.component.MiscApi;
import org.comroid.api.net.luckperms.component.TracksApi;
import org.comroid.api.net.luckperms.component.UserApi;
import org.comroid.api.net.luckperms.model.LuckPermsApiCore;
import org.comroid.api.tree.Component;

import java.util.concurrent.CompletableFuture;

@Value
@Builder
@NonFinal
public class LuckPermsApiWrapper extends Component.Base implements LuckPermsApiCore {
    @lombok.Builder.Default REST rest = REST.Default;
    String         baseUrl;
    Authentication credentials;

    {
        addChildren(new UserApi(this),
                new GroupsApi(this),
                new TracksApi(this),
                //todo: new ActionsApi(this),
                //todo: new MessagingApi(this),
                //todo: new EventApi(this),
                new MiscApi(this));
    }

    @Override
    public REST.Request request(REST.Method method, String path) {
        return rest.new Request(method, baseUrl + path).addHeader("Accept", "application/json")
                .addHeader("Authorization", credentials.toBearerTokenHeader().getValue());
    }

    @Override
    public CompletableFuture<DataNode> get(String path) {
        return request(REST.Method.GET, path).execute()
                .thenApply(REST.Response::validate2xxOK)
                .thenApply(REST.Response::getBody);
    }
}
