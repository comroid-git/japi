package org.comroid.api.net.luckperms.component;

import lombok.Value;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.net.luckperms.model.LuckPermsApiComponent;
import org.comroid.api.net.luckperms.model.LuckPermsApiCore;
import org.comroid.api.net.luckperms.model.ObjectRepository;
import org.comroid.api.net.luckperms.model.track.TrackData;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Value
public class TracksApi extends LuckPermsApiComponent implements ObjectRepository<String, TrackData> {
    public TracksApi(LuckPermsApiCore lpApi) {
        super(lpApi);
    }

    @Override
    public CompletableFuture<Collection<String>> getIDs() {
        return getLpApi().get("/track").thenApply(data -> data.asArray().stream().map(DataNode::asString).toList());
    }

    @Override
    public CompletableFuture<TrackData> get(String name) {
        return getLpApi().get("/track/" + name).thenApply(data -> data.as(TrackData.class).assertion());
    }
}
