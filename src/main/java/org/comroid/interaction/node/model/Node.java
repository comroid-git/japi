package org.comroid.interaction.node.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.comroid.api.attr.Described;
import org.comroid.api.attr.Named;
import org.comroid.interaction.registry.RegistrySource;
import org.jspecify.annotations.NonNull;

@Getter
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
public abstract class Node implements Named, Described {
    @NonNull RegistrySource source;
}
