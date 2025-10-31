package org.comroid.api.net.nextcloud.model.tables;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Value;

import java.time.Instant;
import java.util.Collection;

@Value
public class TableEntry {
    long   id;
    int    tableId;
    String createdBy;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss") Instant createdAt;
    String lastEditBy;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss") Instant lastEditAt;
    Collection<ColumnValue> data;
}
