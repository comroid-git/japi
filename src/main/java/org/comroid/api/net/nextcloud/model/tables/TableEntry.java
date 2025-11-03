package org.comroid.api.net.nextcloud.model.tables;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Collection;

@Value
public class TableEntry {
    long   id;
    int    tableId;
    String createdBy;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt;
    String lastEditBy;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime lastEditAt;
    Collection<ColumnValue> data;
}
