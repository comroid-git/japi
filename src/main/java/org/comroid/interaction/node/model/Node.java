package org.comroid.interaction.node.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.comroid.interaction.registry.RegistrySource;
import org.jspecify.annotations.NonNull;

@Getter
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
public abstract class Node {
    @NonNull RegistrySource source;
}
