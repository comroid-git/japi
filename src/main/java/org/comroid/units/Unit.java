package org.comroid.units;

import org.jetbrains.annotations.NotNull;

import static org.jetbrains.annotations.ApiStatus.*;

@Experimental
public record Unit(String name) {
    @Override
    public @NotNull String toString() {
        return name;
    }
}
