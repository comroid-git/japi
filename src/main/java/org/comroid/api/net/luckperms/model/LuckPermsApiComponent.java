package org.comroid.api.net.luckperms.model;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.comroid.api.tree.Component;

@Value
@NonFinal
public abstract class LuckPermsApiComponent extends Component.Base {
    LuckPermsApiCore lpApi;
}
