package org.comroid.api.net.nextcloud.model;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.comroid.api.tree.Component;

@Value
@NonFinal
public abstract class OcsApiComponent extends Component.Base {
    OcsApiCore ocsApi;
}
