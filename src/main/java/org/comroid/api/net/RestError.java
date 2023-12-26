package org.comroid.api.net;

import lombok.Builder;
import lombok.Data;
import org.comroid.api.data.seri.DataNode;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

@Data
@Builder
public class RestError implements DataNode {
    String error;
    int error_code;
    @Nullable String error_description;
    @Nullable String error_developer_message;
    @Nullable URI error_uri;
}
