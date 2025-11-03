package org.comroid.api.net.nextcloud.model.tables;

import lombok.Value;

@Value
public class ColumnValue {
    int    columnId;
    Object value;
}
