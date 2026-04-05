package org.comroid.interaction.registry;

import lombok.Value;
import org.comroid.api.attr.Named;
import org.comroid.api.func.util.Optionals;
import org.comroid.interaction.model.InteractionTree;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

@Value
public class InstanceRegistry implements RegistrySource {
    @NonNull Object instance;

    @Override
    public String getName() {
        return Optional.of(instance).flatMap(Optionals.cast(Named.class)).map(Named::getName).orElseGet(instance.getClass()::getSimpleName);
    }

    @Override
    public InteractionTree loadTree() {
        return InteractionTree.builder().source(this).nodes(RegistryHelper.compileFromClass(this, instance.getClass())).build();
    }
}
