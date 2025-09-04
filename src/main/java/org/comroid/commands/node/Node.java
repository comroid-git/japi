package org.comroid.commands.node;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.comroid.api.attr.Aliased;
import org.comroid.api.attr.Described;
import org.comroid.api.attr.Named;
import org.comroid.api.func.Specifiable;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

import static java.util.stream.Stream.*;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class Node implements Named, Described, Aliased, Specifiable<Node> {
    @NotNull String name;

    @Override
    public Stream<String> aliases() {
        return concat(Aliased.super.aliases(), of(getName()));
    }
}
