package org.comroid.api.net.luckperms.component;

import lombok.Value;
import org.comroid.api.net.luckperms.model.LuckPermsApiComponent;
import org.comroid.api.net.luckperms.model.LuckPermsApiCore;
import org.comroid.api.net.luckperms.model.dto.HealthResult;

import java.util.concurrent.CompletableFuture;

@Value
public class MiscApi extends LuckPermsApiComponent {
    public MiscApi(LuckPermsApiCore lpApi) {
        super(lpApi);
    }

    public CompletableFuture<HealthResult> getHealth() {
        return getLpApi().get("/health").thenApply(data -> data.as(HealthResult.class).assertion());
    }
}
