package org.comroid.api.net.nextcloud.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import org.comroid.api.func.exc.ThrowingFunction;
import org.comroid.api.func.ext.Context;
import org.comroid.api.net.nextcloud.model.OcsApiComponent;
import org.comroid.api.net.nextcloud.model.OcsApiCore;
import org.comroid.api.net.nextcloud.model.tables.TableEntry;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Value
public class TablesApi extends OcsApiComponent {
    public TablesApi(OcsApiCore ocsApi) {
        super(ocsApi);
    }

    public CompletableFuture<Collection<TableEntry>> getEntries(int tableId) {
        return getOcsApi().get("/index.php/apps/tables/api/1/tables/%d/rows".formatted(tableId))
                .thenApply(data -> data.asArray()
                        .stream()
                        .map(ThrowingFunction.rethrowing(entry -> Context.wrap(ObjectMapper.class)
                                .assertion()
                                .readValue(entry.toSerializedString(),
                                        TableEntry.class)))
                        .filter(Objects::nonNull)
                        .toList());
    }
}
