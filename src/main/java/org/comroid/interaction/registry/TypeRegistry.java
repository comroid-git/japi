package org.comroid.interaction.registry;

import lombok.Value;
import org.comroid.interaction.model.InteractionTree;
import org.jspecify.annotations.NonNull;

@Value
public class TypeRegistry implements RegistrySource {
    @NonNull Class<?> type;

    @Override
    public String getName() {
        return type.getSimpleName();
    }

    @Override
    public InteractionTree loadTree() {
        return InteractionTree.builder().source(this).nodes(RegistryHelper.compileFromClass(this, type)).build();
    }
}
