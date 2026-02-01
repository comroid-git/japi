package org.comroid.api.net.luckperms.model;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface ObjectRepository<ID, T> {
    CompletableFuture<Collection<ID>> getIDs();

    CompletableFuture<T> get(ID key);
}
