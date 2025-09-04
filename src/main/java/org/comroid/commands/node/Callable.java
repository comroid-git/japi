package org.comroid.commands.node;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.comroid.commands.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class Callable extends Node {
    @NotNull Command attribute;

    public @NotNull String getName() {
        return Command.EmptyAttribute.equals(attribute.value()) ? Optional.of(super.getName())
                .or(() -> Optional.ofNullable(getAlternateName()))
                .orElseThrow(() -> new NullPointerException("No name defined for command " + this)) : attribute.value();
    }

    @Override
    public String getAlternateName() {
        return null; // stub to prevent recursion loop
    }

    public abstract @Nullable Call asCall();

    public abstract Stream<? extends Node> nodes();
}
